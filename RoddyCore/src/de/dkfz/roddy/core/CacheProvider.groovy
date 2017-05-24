///*
// * Copyright (c) 2016 eilslabs.
// *
// * Distributed under the MIT License (license terms are at https://www.github.com/eilslabs/Roddy/LICENSE.txt).
// */
//
//package de.dkfz.roddy.core
//
//import javafx.collections.FXCollections
//import javafx.collections.ObservableList;
//
///**
// * Base class for classes which provide caching mechanisms.
// * All of those classes are registered here.
// * The class allows the release (and then the rebuild) of caches.
// */
//@groovy.transform.CompileStatic
//public abstract class CacheProvider extends Initializable {
//
//    private static final ObservableList<CacheProvider> allCacheProviders = FXCollections.<CacheProvider>synchronizedObservableList(FXCollections.<CacheProvider>observableArrayList());
//    private static final Map<String, CacheProvider> cacheProvidersByName = [:];
//    private final String id;
//
//    private List<CacheProviderListener> listeners = new LinkedList<>();
//
//    public static interface CacheProviderListener {
//        public void cacheValueAdded(CacheProvider source, String name, String value);
//
//        public void cacheValueChanged(CacheProvider source, String name, String value);
//
//        public void cacheValueRead(CacheProvider source, String name, int noOfReads);
//    }
//
//    protected CacheProvider(String id, boolean unique) {
////        final CacheProvider THIS = this;
////        if (unique) {
////            try {
////
////
////                Collection<CacheProvider> all = allCacheProviders.findAll {
////                    Object cp ->
////                        return cp.class == THIS.class;
////                }
////                allCacheProviders.removeAll(all);
////            } catch (Exception ex) {
////                println(ex);
////            }
////        }
//        this.id = id;
////        allCacheProviders.add(this);
//    }
//
//    protected CacheProvider(String id) {
//        this(id, false);
//    }
//
//    /**
//     * Releases the cache in this provider
//     */
//    public abstract void releaseCache();
//
//    /**
//     * Releases the caches in all registered cache providers.
//     */
//    public static void releaseAllCaches() {
//        allCacheProviders.each { CacheProvider provider -> provider.releaseCache(); }
//    }
//
//    /**
//     * Returns a cache provider or null.
//     * @param name
//     * @return
//     */
//    public static CacheProvider getProvider(String name) {
//        return cacheProvidersByName[name];
//    }
//
//    public static ObservableList<CacheProvider> getAllCacheProviders() {
//        return allCacheProviders;
//    }
//
//    public void addListener(CacheProviderListener listener) {
//        if (this.listeners.contains(listener)) return;
//        this.listeners << listener;
//    }
//
//    protected void fireCacheValueAddedEvent(String id, String val) {
//        for (CacheProviderListener it in listeners) {
//            it.cacheValueAdded(this, id, val);
//        }
//    }
//
//    protected void fireCacheValueChangedEvent(String id, String val) {
//        for (CacheProviderListener it in listeners) {
//            it.cacheValueAdded(this, id, val);
//        }
//    }
//
//    protected void fireCacheValueReadEvent(String id, int readCount) {
//        for (CacheProviderListener it in listeners) {
//            it.cacheValueRead(this, id, readCount);
//        }
//    }
//
//    public String getID() {
//        return id;
//    }
//}
