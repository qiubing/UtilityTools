import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by bing on 2017-06-10.
 * 用来保存强引用的缓存，最近访问的对象被移至队列的首部，
 * 最近最少访问的对象移至队列的尾部，当缓存满的时候，最先清除队尾缓存项。
 * LruCache非常适合用做内存缓存
 */
public class LruCache<K,V> {
    private int size;
    private int maxSize;
    private final LinkedHashMap<K,V> map;

    //统计
    private int putCount;//put的缓存数量
    private int evictionCount;//失效的缓存数量
    private int hitCount;//get查询到的缓存数量
    private int missCount;//get查询到失效的缓存数量

    public LruCache(int maxSize){
        if (maxSize < 0){
            throw new IllegalArgumentException("maxSize <= 0");
        }
        this.maxSize = maxSize;
        //以访问者顺序
        this.map = new LinkedHashMap<K, V>(0,0.75f,true);
    }

    /**
     * 返回包含key和value的Entry大小
     * 默认实现是返回1,这样size就是已经存入元素的个数，maxSize表示为最大支持的元素数量
     * 可以自定义实现该方法来定义计算Entry的大小
     * @param key
     * @param value
     * @return
     */
    protected int sizeOf(K key,V value){
        return 1;
    }

    /**
     * 当缓存项被移除时，回调该方法。
     * 用户可以定义实现该方法，在缓存项被移除时，回调该方法。
     * @param evicted 如果evicted为true，则表示该缓存项移除是为了释放缓存空间，
     *                如果evicted为false，则表示是remove和put方法引起的缓存项被移除
     * @param key 被移除缓存项的key值
     * @param oldValue 被移除缓存项原来对应的值
     * @param newValue 被替换缓存项新的值
     */
    protected void entryRemoved(boolean evicted,K key,V oldValue,V newValue){}

    /**
     * 缓存key-value值，并把Value值移至队列的首部。
     * @param key
     * @param value
     * @return
     */
    public final V put(K key,V value){
        if (key == null || value == null){
            throw new IllegalArgumentException("key == null || value == null");
        }

        V previous;
        synchronized (this){
            putCount++;
            size += safeSizeOf(key,value);
            previous = map.put(key,value);
            //如果是替换原来的值，则需要把原来元素的大小减去
            if (previous != null){
                size -= safeSizeOf(key,previous);
            }
        }
        //缓存项有移除，回调entryRemoved方法
        if (previous != null){
            entryRemoved(false,key,previous,value);
        }

        trimToSize(maxSize);
        return previous;
    }

    /**
     * 安全返回一个entry的大小，避免出现负值的情况
     * @param key
     * @param value
     * @return
     */
    private int safeSizeOf(K key,V value){
        int result = sizeOf(key,value);
        if (result < 0){
            throw new IllegalArgumentException("Negative size: " + key + " = " + value);
        }
        return result;
    }

    /**
     * 将缓存的最大大小调整到maxSize
     * 因为添加一个新的元素，会改变缓存的size，可能导致超出最大maxSize，这时就需要移除一些旧的缓存，腾出空间来存储新的entry值.
     * @param maxSize 缓存的最大大小
     */
    private void trimToSize(int maxSize){
        while(true){
            K key;
            V value;
            synchronized (this){
                if (size < 0 || (map.isEmpty() && size != 0)){
                    throw new IllegalStateException(getClass().getName()
                            + ".sizeOf() is reporting inconsistent results!");
                }

                //如果缓存队列没有满，则直接返回
                if (size < maxSize){
                    break;
                }

                Map.Entry<K,V> toEvict = null;
                //循环遍历LinkedHashMap，获取队尾的entry，并把该entry赋值给toEvict，该entry是需要被回收的
                for (Map.Entry<K,V> entry : map.entrySet()){
                    toEvict = entry;
                }

                key = toEvict.getKey();
                value = toEvict.getValue();
                map.remove(key);
                size -= safeSizeOf(key,value);
                evictionCount++;
            }
            //回调缓存被移除的回调接口，这是由于缓存满了，需要移除缓存项释放空间，所以evicted为true，newValue为null
            entryRemoved(true,key,value,null);
        }
    }


    /**
     * 根据key获取value值
     * @param key
     * @return
     */
    public V get(K key){
        if (key == null){
            throw new NullPointerException("key == null");
        }

        V value;
        synchronized (this){
            value = map.get(key);
            if (value != null){
                hitCount++;
                return value;
            }
            missCount++;
        }
        return null;
    }

    public final V remove(K key){
        if (key == null){
            throw  new IllegalArgumentException("key == null");
        }

        V previous;
        synchronized (this){
            previous = map.remove(key);
            if (previous != null){
                size -= safeSizeOf(key,previous);
            }
        }
        if (previous != null){
            entryRemoved(false,key,previous,null);
        }
        return previous;
    }

    /**
     * 移除所有的缓存项
     */
    public final void evictAll(){
        trimToSize(-1);
    }

    /**
     * 获取缓存项的大小
     * @return
     */
    public synchronized final int size(){
        return size;
    }

    public synchronized final int maxSize(){
        return maxSize;
    }

    public synchronized final int putCount(){
        return putCount;
    }

    public synchronized final int hitCount(){
        return hitCount;
    }

    public synchronized final int missCount(){
        return missCount;
    }

    public synchronized final int evictionCount(){
        return evictionCount;
    }

    @Override
    public String toString() {
        int access = hitCount + missCount;
        int hitPresent = access != 0 ? (100 * hitCount / access) : 0;
        return String.format("LruCache[maxSize=%d,hits=%d,misses=%d,hitRate=%d%%]",
                maxSize,hitCount,missCount,hitPresent);
    }
}
