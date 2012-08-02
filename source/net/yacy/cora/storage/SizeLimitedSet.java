/**
 *  SizeLimitedSet
 *  Copyright 2012 by Michael Peter Christen, mc@yacy.net, Frankfurt a. M., Germany
 *  First released 04.07.2012 at http://yacy.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program in the file lgpl21.txt
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package net.yacy.cora.storage;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public class SizeLimitedSet<E> extends AbstractSet<E> implements Set<E>, Cloneable, Serializable {

	private static final long serialVersionUID = -1674392695322189500L;

	private transient SizeLimitedMap<E,Object> map;

    private static final Object OBJECT = new Object();

    public SizeLimitedSet(int sizeLimit) {
        map = new SizeLimitedMap<E,Object>(sizeLimit);
    }
    
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    public boolean add(E e) {
        return map.put(e, OBJECT) == null;
    }

    public boolean remove(Object o) {
        return map.remove(o) == OBJECT;
    }

    public void clear() {
        map.clear();
    }

    @SuppressWarnings("unchecked")
	public Object clone() {
        try {
        	SizeLimitedSet<E> n = (SizeLimitedSet<E>) super.clone();
            n.map = (SizeLimitedMap<E, Object>) map.clone();
            return n;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

	
}
