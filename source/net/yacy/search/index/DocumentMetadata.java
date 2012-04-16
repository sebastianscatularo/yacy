/**
 *  DocumentMetadata
 *  Copyright 2012 by Michael Peter Christen
 *  First released 3.4.2012 at http://yacy.net
 *
 *  This file is part of YaCy Content Integration
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

package net.yacy.search.index;

import java.io.IOException;

import net.yacy.cora.order.CloneableIterator;
import net.yacy.cora.sorting.WeakPriorityBlockingQueue.Element;
import net.yacy.kelondro.data.meta.URIMetadata;
import net.yacy.kelondro.data.word.WordReference;

public class DocumentMetadata implements Metadata {

	@Override
	public void clearCache() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clear() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int writeCacheSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public URIMetadata load(Element<WordReference> obrwi) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URIMetadata load(byte[] urlHash) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void store(URIMetadata entry) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean remove(byte[] urlHashBytes) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean exists(byte[] urlHash) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CloneableIterator<byte[]> keys(boolean up, byte[] firstKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CloneableIterator<URIMetadata> entries() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CloneableIterator<URIMetadata> entries(boolean up, String firstHash)
			throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int deleteDomain(String hosthash) throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
