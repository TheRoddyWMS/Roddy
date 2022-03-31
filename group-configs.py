#!/bin/env python3
#
# Copyright (c) 2021 German Cancer Research Center (DKFZ).
#
# Distributed under the MIT License (license terms are at https://github.com/DKFZ-ODCF/AlignmentAndQCWorkflows).
#
# $ group-configs.py path/to/ConfigSummary.py roddyExecutionStore1 roddyExecutionStore2 ...
#
# Compiles all configurations from all provided roddyExecutionStore directories.
#
# * Output is JSON in which the simplified/essential configuration of the workflow for a set of directories
#   is displayed (=contexts).
# * If all .parameter files of an exec_* directory have the same configuration, only the exec_* directory
#   is reported in the "contexts" field.
# * If all exec_* directories in a roddyExecutionStore have the same configuration, only the
#   roddyExecutionStore is reported in the "contexts" field.
#
# Run the unit-tests with `pytest group-configs.py`
#
# Requirements: python2, more_itertools, pytest (for testing)
#
# Disclaimer: The plugin underwent quite some changes over the years. Use at your own responsibility.
#             If you find errors, please file a bug report, such that the script gets improved.
#
# TODO Diffing reports between groups to highlight differences for the user.
#
from __future__ import annotations

import os
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Tuple, Union, Optional, TextIO, Mapping, Callable

import json
import logging
import re
from abc import ABCMeta, abstractmethod
from io import StringIO
from more_itertools import flatten
import importlib

logger = logging.Logger(__name__)


def parse_parameter_line(line: str) -> Union[Tuple[str, str], None]:
    """
    Parse a line from Roddy's .parameter files.
    """
    match = re.match(r"declare(?:\s-\S)*\s+([^=]+)=(.+)", line.rstrip())
    if match:
        return match.groups()[0], match.groups()[1]
    else:
        return None


def test_parse_parameter_line():
    assert parse_parameter_line("if [[ -z \"${PS1-}\" ]]; then") is None
    assert parse_parameter_line("declare -x -i TARGETSIZE=74569526") == \
           ("TARGETSIZE", "74569526")
    assert parse_parameter_line("declare -x -i BWA_MEM_THREADS=8") == \
           ("BWA_MEM_THREADS", "8")
    assert parse_parameter_line("declare -x    SAMBAMBA_MARKDUP_VERSION=0.5.9") == \
           ("SAMBAMBA_MARKDUP_VERSION", "0.5.9")
    assert parse_parameter_line("declare -x    SAMBAMBA_MARKDUP_OPTS=\"-t 6 -l 9 " +
                                "--hash-table-size=2000000 --overflow-list-size=1000000 " +
                                "--io-buffer-size=64\"") == \
           ("SAMBAMBA_MARKDUP_OPTS",
            "\"-t 6 -l 9 --hash-table-size=2000000 --overflow-list-size=1000000 --io-buffer-size=64\"")
    assert parse_parameter_line("\n") is None


def parse_parameter_file(file: Path) -> Mapping[str, str]:
    """
    Parse the full parameter file and return a dictionary of variable names and values.
    Will return an empty dictionary, if no parameters can be parsed.
    """
    result = {}
    with open(file, "r") as f:
        for line in f.readlines():
            parsed = parse_parameter_line(line)
            if parsed is not None:
                result[parsed[0]] = parsed[1]
    return result


def read_version_info(input: TextIO) -> Mapping[str, str]:
    """
    Read a version info file and return a dictionary with the component (Roddy, plugins) and their
    versions parsed from the file.
    """
    result = {}
    first_line = input.readline().rstrip()
    match = re.match(r"Roddy version: (\d+\.\d+\.\d+|develop)", first_line)
    if not match:
        raise RuntimeError(f"Couldn't parse Roddy version from '{first_line}'")
    result["Roddy"] = match.groups()[0]
    input.readline()    # Drop "Library info:"
    for line in map(lambda l: l.rstrip(), input.readlines()):
        if line != "":
            match = re.match(r"Loaded plugin (\S+):(\S+)\sfrom.+", line)
            if not match:
                logger.error(f"Could not match plugin in: '{line}'")
            else:
                result[match.groups()[0]] = match.groups()[1]
    return result


def test_read_version_info():
    buffer = StringIO(
        """Roddy version: 3.5.9
Library info:
Loaded plugin AlignmentAndQCWorkflows:1.2.51-1 from ((/tbi/software/x86_64/otp/roddy/plugins/3.5/AlignmentAndQCWorkflows_1.2.51-1))
Loaded plugin COWorkflows:1.2.66-1 from ((/tbi/software/x86_64/otp/roddy/plugins/3.5/COWorkflows_1.2.66-1))
Loaded plugin PluginBase:1.2.1-0 from ((/tbi/software/x86_64/otp/roddy/roddy/3.5.9/dist/plugins/PluginBase_1.2.1))
Loaded plugin DefaultPlugin:1.2.2-0 from ((/tbi/software/x86_64/otp/roddy/roddy/3.5.9/dist/plugins/DefaultPlugin_1.2.2))
        """)
    parsed = read_version_info(buffer)
    assert parsed == {
        "Roddy": "3.5.9",
        "AlignmentAndQCWorkflows": "1.2.51-1",
        "COWorkflows": "1.2.66-1",
        "PluginBase": "1.2.1-0",
        "DefaultPlugin": "1.2.2-0"
    }


@dataclass
class ParameterSet:
    """
    The contexts are simply a list of directories for which the given parameters are valid.
    The client code may choose to simplify the context to a super-directory, to reduce the
    complexity of the output.
    """
    contexts: List[Path]
    parameters: Optional[Mapping[str, Mapping[str, Optional[str]]]]


class JobParameterEncoder(json.JSONEncoder):
    """
    An encoder to convert a JobParameter into a nested dict/list structure that can be serialized.
    """
    def default(self, obj):
        if isinstance(obj, ParameterSet):
            return {
                "contexts": list(map(str, obj.contexts)),
                "parameters": obj.parameters
            }
        # Let the base class default method raise the TypeError
        return json.JSONEncoder.default(self, obj)


@dataclass
class ExecutionStore:
    """
    A representation of a roddyExecutionStore and the information logged in there.
    """
    roddy_store_path: Path
    execution_store_subdir: Path
    parameter_file_names: List[Path]
    parameters: List[ParameterSet] = field(default_factory=list)

    # The following methods return the path to the respective files/dirs starting with the
    # roddy_store_path.
    @property
    def execution_store(self) -> Path:
        return self.roddy_store_path / self.execution_store_subdir

    @property
    def versions_info_file(self) -> Path:
        return self.execution_store / "versionsInfo.txt"

    @property
    def parameter_files(self) -> List[Path]:
        return list(map(lambda p: self.execution_store / p,
                        self.parameter_file_names))

    def __repr__(self):
        result = [f"Execution store = {str(self.execution_store)}",
                  f"Version infos = {str(self.versions_info_file)}",
                  "Parameter files ="]
        for f in self.parameter_files:
            result.append(f"\t{str(f)}")

    # Factory methods and supplementary code.
    @classmethod
    def _collect_parameter_files(cls, execution_store: Path) -> List[Path]:
        return list(map(lambda f: f.relative_to(execution_store),
                        filter(lambda p: p.is_file() and p.name.endswith(".parameters"),
                               execution_store.iterdir())))

    @classmethod
    def from_path(cls, roddy_store_dir: Path, execution_store_subdir: Path) -> ExecutionStore:
        return ExecutionStore(
            roddy_store_path=roddy_store_dir,
            execution_store_subdir=execution_store_subdir,
            parameter_file_names=cls._collect_parameter_files(roddy_store_dir /
                                                              execution_store_subdir))


@dataclass
class RoddyStore:
    base_dir: Path
    execution_stores: List[ExecutionStore]
    # Whether there is a list or a single JobParameters object depends on whether the values are
    # consistent.
    parameters: List[ParameterSet] = field(default_factory=list)

    def __repr__(self):
        result = [f"Base directory = {self.base_dir}"]
        for store in self.execution_stores:
            result += store.__repr__()
        return "\n\n".join(result)

    # Factory methods and supplementary code.
    @classmethod
    def _collect_execution_dirnames(cls, roddy_store_path: Path) -> List[Path]:
        """
        Return dir names of the exec_ directories in the roddy_store_path.
        """
        return list(map(lambda p: Path(p.name),
                        filter(lambda p: ((p.is_dir() or p.is_symlink()) and
                                          p.name.startswith("exec_")),
                               roddy_store_path.iterdir())))

    @classmethod
    def from_path(cls, base_dir: Path) -> RoddyStore:
        stores = []
        for exec_store_subdir in cls._collect_execution_dirnames(base_dir):
            stores.append(ExecutionStore.from_path(base_dir, exec_store_subdir))
        if len(stores) == 0:
            print(f"No exec_* subdirectories in '{base_dir.name}. Will be ignored!")
        return RoddyStore(base_dir=base_dir,
                          execution_stores=stores)


def simple_combine_parameter_sets(parameters: List[ParameterSet]) \
        -> List[ParameterSet]:
    """
    This is a strategy for combining parameter sets. It's very simple: Just check whether all
    parameters are identical and return one representative, if they are. Otherwise, return the
    original input.
    """
    first_parameters = parameters[0].parameters
    consistent = all(map(lambda job: job.parameters == first_parameters,
                         parameters))
    if consistent:
        return [ParameterSet(contexts=list(flatten(map(lambda p: p.contexts,
                                                       parameters))),
                             parameters=first_parameters)]
    else:
        return parameters


def test_simple_combine_parameter_sets():
    # Two equal
    assert simple_combine_parameter_sets([ParameterSet(contexts=[Path("b")],
                                                       parameters={"b": 1}),
                                          ParameterSet(contexts=[Path("c")],
                                                       parameters={"b": 1})]) == \
           [ParameterSet(contexts=[Path("b"), Path("c")],
                         parameters={"b": 1})]

    # Two unequal
    assert simple_combine_parameter_sets([ParameterSet(contexts=[Path("b")],
                                                       parameters={"b": 2}),
                                          ParameterSet(contexts=[Path("c")],
                                                       parameters={"b": 1})]) == \
           [ParameterSet(contexts=[Path("b")],
                         parameters={"b": 2}),
            ParameterSet(contexts=[Path("c")],
                         parameters={"b": 1})]

    # Three, two equal, one not
    assert simple_combine_parameter_sets([ParameterSet(contexts=[Path("b")],
                                                       parameters={"b": 2}),
                                          ParameterSet(contexts=[Path("c")],
                                                       parameters={"b": 1}),
                                          ParameterSet(contexts=[Path("d")],
                                                       parameters={"b": 2})]) == \
           [ParameterSet(contexts=[Path("b")],
                         parameters={"b": 2}),
            ParameterSet(contexts=[Path("c")],
                         parameters={"b": 1}),
            ParameterSet(contexts=[Path("d")],     # This one is not grouped with the first!
                         parameters={"b": 2})]


def grouping_combine_parameter_sets(job_parameters: List[ParameterSet]) \
        -> List[ParameterSet]:
    """
    This strategy for combining results, makes an all vs. all comparison of parameters and groups
    those that are identical. The contexts (usually exec_* dirs) are compiled in a list.
    """
    # We use the string-mapped parameters, because we want to parameters that are *identical*.
    mapped_params = map(lambda jp: (json.dumps(jp.parameters, sort_keys=True), jp),
                        job_parameters)

    params_by_params: dict = {}
    for key, params in mapped_params:
        if key in params_by_params.keys():
            # Accumulate the contexts for these parameters.
            params_by_params[key] = {
                "contexts": params_by_params[key]["contexts"] + params.contexts,
                "parameters": params_by_params[key]["parameters"]
            }
        else:
            # Initialize the context for these parameters.
            params_by_params[key] = {
                "contexts": params.contexts,
                "parameters": params.parameters
            }

    result = list(map(lambda p: ParameterSet(**p), params_by_params.values()))
    return result


def test_grouping_combine_parameter_sets():
    # Two equal
    assert grouping_combine_parameter_sets([ParameterSet(contexts=[Path("b")],
                                                         parameters=dict({"b": 1})),
                                            ParameterSet(contexts=[Path("c")],
                                                         parameters=dict({"b": 1}))]) == \
           [ParameterSet(contexts=[Path("b"), Path("c")],
                         parameters=dict({"b": 1}))]

    # Two unequal
    assert grouping_combine_parameter_sets([ParameterSet(contexts=[Path("b")],
                                                         parameters={"b": 2}),
                                            ParameterSet(contexts=[Path("c")],
                                                         parameters={"b": 1})]) == \
           [ParameterSet(contexts=[Path("b")],
                         parameters={"b": 2}),
            ParameterSet(contexts=[Path("c")],
                         parameters={"b": 1})]

    # Three, two equal, one not
    assert grouping_combine_parameter_sets([ParameterSet(contexts=[Path("b")],
                                                         parameters={"b": 2}),
                                            ParameterSet(contexts=[Path("c")],
                                                         parameters={"b": 1}),
                                            ParameterSet(contexts=[Path("d")],     # same paras as b!
                                                         parameters={"b": 2})]) == \
           [ParameterSet(contexts=[Path("b"), Path("d")],    # b and d are grouped
                         parameters={"b": 2}),
            ParameterSet(contexts=[Path("c")],
                         parameters={"b": 1})]


class ExecutionStoreAnalyzer:
    """
    A `roddyExecutionStore` directory may contain multiple exec_* directories.

    Multiple execution stores may exist for an analysis directory, if the workflow was run multiple
    times. This function tries to combine the information from multiple such runs. If the
    information cannot be reconciled. The rules are as follows

    * Versions in later runs override versions in earlier runs.
    * Cluster jobs are generally reported separately, unless they all used the same versions.
    """

    def __init__(self,
                 summary_function: Callable[[Mapping[str, str], Mapping[str, str]],
                                            Mapping[str, Mapping[str, Optional[str]]]]):
        """
        The summary function should take two parameters

        * A dictionary mapping plugins and Roddy to versions
        * A dictionary of configuration values.

        It should return a dictionary structure, representing the summarized and condensed configuration.
        """
        self._interpret_parameters = summary_function

    def run(self, roddy_store: RoddyStore) -> List[ParameterSet]:
        """
        Collect version and other run information from all execution store directories. For each
        exec_ directory, return a dictionary of key value pairs with information. Information is
        left out, if they are not relevant. E.g. a software version may be omitted, if the software
        was not used, because it was turned off some other parameter value.
        """
        per_exec_store: List[ParameterSet] = []
        for exec_store in roddy_store.execution_stores:
            with open(exec_store.versions_info_file, "r") as f:
                versions = read_version_info(f)
            if len(exec_store.parameter_files) == 0:
                store_parameters = [ParameterSet(contexts=[roddy_store.base_dir],
                                                 parameters={})]
            else:
                job_parameters: List[ParameterSet] = []
                for job_parameters_file in exec_store.parameter_files:
                    raw_parameters = parse_parameter_file(job_parameters_file)
                    job_parameters.append(ParameterSet([job_parameters_file],
                                                       self._interpret_parameters(versions,
                                                                                  raw_parameters)))

                # In principle, each job may have different parameters. We first combine the parameters
                # of all .parameter files. This is basically done by rejecting the exec_* directory
                # because finding divergent parameters is a serious sign of messing with Roddy, which
                # we don't support.
                store_parameters = grouping_combine_parameter_sets(job_parameters)
                if len(store_parameters) > 1:
                    # We issue an error, if different parameters are used for different workflow runs
                    # on the same output directory, because this is a serious indication of messing
                    # with Roddy's execution. But we do not stop the whole analysis.
                    print(f"WARNING: Inconsistent job parameters in {exec_store.execution_store}. Ignored!",
                          file=sys.stderr)
                else:
                    # To simplify the output, we replace the list of parameter files by the execution
                    # store directory. This expresses also to the client, that all execution store
                    # directories contain the same parameter set.
                    store_parameters = [ParameterSet(contexts=[Path(exec_store.execution_store)],
                                                     parameters=store_parameters[0].parameters)]

            exec_store.parameters = store_parameters
            per_exec_store += store_parameters

        # What is much more likely is that the configurations from different executions of Roddy,
        # differ, even though, usually, all jobs will have the same parameters. We try to combine
        # all parameters from all executions, but if that is not possible, report all variants.
        roddy_store_parameters = grouping_combine_parameter_sets(per_exec_store)

        # Like before, we look at all execution stores, and if all have the same parameters,
        # we can simplify to only list the roddyExecutionStore directory as context.
        if len(roddy_store_parameters) == 1:
            roddy_store_parameters = [ParameterSet(contexts=[roddy_store.base_dir],
                                                   parameters=roddy_store_parameters[0].parameters)]

        roddy_store.parameters = roddy_store_parameters
        return roddy_store_parameters


def load_module_from_path(path: Path):
    """
    Import the requested ConfigSummary implementation (should fit to the for the executionStores).
    Compare https://stackoverflow.com/a/28590138/8784544
    """
    sys.path.append(os.path.dirname(config_summary_path))
    module_name = os.path.splitext(os.path.basename(config_summary_path))[0]
    imported_module = importlib.import_module(module_name)
    sys.path.pop()
    return imported_module


if __name__ == "__main__":
    """
    Just call the script with a path to a configuration-summary module and a set of `roddyExecutionStore` directories,
    collect the configuration information and generate a report.
    
    If all directories and recursively, exec_* directories and .parameter files used the same
    configuration, only this config is returned in an interpreted way that minimizes the output.
    
    If there are exec_* directories with different configurations in the same roddyExecutionStore
    then they will be reported separately (the configurations are inconsistent).
    
    If there are even .parameter files in the same exec_* directory, you messed up a single run
    of Roddy. S.b. probably messed around with the configs. Pray that the results are usable!
    
    DISCLAIMER: This script can only find version information as used by the workflow itself. If 
                you hacked and ran partial analyses manually with other tools, then this script
                will produce wrong results.
    """
    if len(sys.argv) < 3:
        print("Usage: group-configs.py pathToConfigSummaryModule execStore+", file=sys.stderr)
        sys.exit(1)

    config_summary_path = Path(sys.argv[1])
    imported_module = load_module_from_path(config_summary_path)

    directories = sys.argv[2:]

    # The module is expected to contain a ConfigSummary class with a run method. With the signature
    # def run(self,
    #         plugin_versions: Mapping[str, str],
    #         parameters: Mapping[str, str]) -> \
    #         Mapping[str, Mapping[str, Optional[str]]]
    analyzer = ExecutionStoreAnalyzer(imported_module.ConfigSummary().summarize)

    per_roddy_store = []
    for roddy_execution_store_dir in directories:
        print("Analysing '%s' ..." % roddy_execution_store_dir, file=sys.stderr)
        exec_store = RoddyStore.from_path(Path(roddy_execution_store_dir))
        per_roddy_store += analyzer.run(exec_store)

    combined: List[ParameterSet] = grouping_combine_parameter_sets(per_roddy_store)
    if len(combined) > 1:
        print("Could not combine configurations in roddyExecutionStores", file=sys.stderr)
        json.dump(combined, sys.stdout, cls=JobParameterEncoder)
        sys.exit(2)
    else:
        print("Same configuration in roddyExecutionStores", file=sys.stderr)
        json.dump(combined, sys.stdout, cls=JobParameterEncoder)
        sys.exit(0)


