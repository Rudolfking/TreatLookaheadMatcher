//package hu.bme.mit.inf.lookaheadmatcher.impl;
//
//
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.Iterator;
//import java.util.Map;
//import java.util.Map.Entry;
//
///*
// * Like a set but some items can be in it more than once. If more equal items are added, we count its instances and
// * add or increment it. Removing decrements the count, except if it is the last one (then remove the item itself).
// * So all items has a corresponding count property, represented in a Map<item, count>.
// * Extra features: generic, addAll(), ToArrayList(boolean) (if param is false (or not given), multiple items will
// * be represented only once!!!!)
// */
//public class MultiSet<T>
//{
//    private Map<T,Integer> counts;
//
//    public MultiSet()
//    {
//        counts = new HashMap<T,Integer>();
//    }
//    
//    public Map<T,Integer> getInnerMap()
//    {
//    	return this.counts;
//    }
//
//    @Override
//    public String toString()
//    {
//        return counts.toString();
//    }
//
//    /**
//     * Returns the size including the multiplicates
//     */
//    public int size()
//    {
//        int total = 0;
//
//        for (Integer occurs : counts.values())
//            total += occurs;
//
//        return total;
//    }
//
//    /**
//     * Returns the size excluding the multiplicates
//     */
//    public int uniqueSize()
//    {
//        return counts.size();
//    }
//
//    public void add(T x)
//    {
//        Integer occurs = counts.get(x);
//
//        if(occurs == null)
//            counts.put(x, 1);
//        else
//            counts.put(x, occurs + 1);
//    }
//    
//    public void addAll(Collection<? extends T> x)
//    {
//        Integer occurs = counts.get(x);
//
//        for (T t : x) {
//            if(occurs == null)
//                counts.put(t, 1);
//            else
//                counts.put(t, occurs + 1);
//		}
//    }
//
//    /** Remove an item.
//     * If x is not found, return false.
//     * If x is present with count 1, remove it from the map.
//     * If x is present with count > 1, drop count
//     */
//    public boolean remove(T x)
//    {
//        Integer occurs = counts.get(x);
//
//        if (occurs == null)
//            return false;
//
//        if (occurs == 1)
//            counts.remove(x);
//        else
//            counts.put(x, occurs - 1);
//
//        return true;
//    }
//
//    /**
//     * Removes an instalce, even it was inside the map multiple times.
//     * @param key To remove
//     * @return The number of entries removed (0 if not found)
//     */
//	public int removeAll(T key)
//	{
//		Integer inside = counts.get(key);
//		if (inside == null)
//			return 0;
//		int removal = 0;
//		while(inside != null)
//		{
//			remove(key);
//			inside = counts.get(key);
//			removal++;
//		}
//		return removal;
//	}
//
//    public boolean contains(T x)
//    {
//        return counts.get(x) != null;
//    }
//    
//    // multipleItemsExpanded = false ("default value")
//    public ArrayList<T> toArrayListDeprecated()
//    {
//    	ArrayList<T> retArray = new ArrayList<T>();
//    	for (T t : counts.keySet()) {
//			retArray.add(t);
//		}
//    	return retArray;
//    }
//    
//    public ArrayList<T> toArrayList(boolean multipleItemsExpanded)
//    {
//    	if (multipleItemsExpanded == false)
//    		return toArrayListDeprecated();
//    	else
//    	{
//        	ArrayList<T> retArray = new ArrayList<T>();
//    		for (Entry<T, Integer> entry : counts.entrySet())
//    		{
//    			for(int i = 0; i < entry.getValue(); i++)
//    				retArray.add(entry.getKey());
//    		}
//    		return retArray;
//    	}
//    }
//
//	public void addAll(MultiSet<T> collection)
//	{
//		for (T elem : collection.toArrayList(true))
//		{
//			this.add(elem);
//		}
//	}
//}
