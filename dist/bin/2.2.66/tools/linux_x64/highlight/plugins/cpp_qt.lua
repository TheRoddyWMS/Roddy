--[[
Sample plugin file for highlight 3.1

Adds additional keywords to C++ syntax description and corresponding
formatting in colour theme
]]

Description="Add Qt keywords to C and C++ definition"

-- optional parameter: syntax description
function syntaxUpdate(desc)
  if desc=="C and C++" then
	-- insert Qt keywords
	table.insert( Keywords,
                  { Id=1, List={"slots" }
                  } )
	table.insert( Keywords,
                  { Id=2, Regex=[[Q[A-Z]\w+]]
                  } )
	table.insert( Keywords,
                  { Id=5, List={"SIGNAL", "SLOT"}
                  } )
	--table.insert( Keywords,
         --         { Id=5, Regex=[[Q_[A-Z]+_]+]]
          --        } )
  end
end

-- optional parameter: theme description
function themeUpdate(desc)
 -- if table.getn(Keywords)==4 then
 --   table.insert(Keywords, {Colour= "#ff0000", Bold=true})
 -- end
end


--The Plugins array assigns code chunks to themes or language definitions.
--The chunks are interpreted after the theme or lang file were parsed,
--so you can refer to elements of these files

Plugins={

  { Type="theme", Chunk=themeUpdate },
  { Type="lang", Chunk=syntaxUpdate },

}
