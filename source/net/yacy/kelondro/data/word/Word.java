// Word.java
// (C) 2008 by Michael Peter Christen; mc@yacy.net, Frankfurt a. M., Germany
// first published 26.03.2008 on http://yacy.net
//
// This is a part of YaCy, a peer-to-peer based web search engine
//
// $LastChangedDate$
// $LastChangedRevision$
// $LastChangedBy$
//
// LICENSE
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package net.yacy.kelondro.data.word;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import net.yacy.cora.storage.ARC;
import net.yacy.cora.storage.ConcurrentARC;
import net.yacy.document.LargeNumberCache;
import net.yacy.kelondro.index.HandleSet;
import net.yacy.kelondro.index.RowSpaceExceededException;
import net.yacy.kelondro.logging.Log;
import net.yacy.kelondro.order.Base64Order;
import net.yacy.kelondro.order.Bitfield;
import net.yacy.kelondro.order.Digest;
import net.yacy.kelondro.util.MemoryControl;


public class Word {


    /**
     * this is the lenght(12) of the hash key that is used:<br>
     * - for seed hashes (this Object)<br>
     * - for word hashes (IndexEntry.wordHashLength)<br>
     * - for L-URL hashes (plasmaLURL.urlHashLength)<br><br>
     * these hashes all shall be generated by base64.enhancedCoder
     */
    public static final int commonHashLength = 12;

    private static final int hashCacheSize = Math.max(200000, Math.min(10000000, (int) (MemoryControl.available() / 20000L)));
    private static ARC<String, byte[]> hashCache = null;
    static {
        try {
            hashCache = new ConcurrentARC<String, byte[]>(hashCacheSize, Math.max(32, 4 * Runtime.getRuntime().availableProcessors()));
        } catch (final OutOfMemoryError e) {
            hashCache = new ConcurrentARC<String, byte[]>(1000, Math.max(8, 2 * Runtime.getRuntime().availableProcessors()));
        }
    }
    /*
    private static ConcurrentHashMap<String, byte[]> hashCache = null;
    static {
        hashCache = new ConcurrentHashMap<String, byte[]>();
    }
    */

    // object carries statistics for words and sentences
    public  int      count;       // number of occurrences
    public  int      posInText;   // unique handle, is initialized with word position (excluding double occurring words)
    public  int      posInPhrase; // position of word in phrase
    public  int      numOfPhrase; // number of phrase. 'normal' phrases begin with number 100
    Set<Integer> phrases;         // a set of handles to all phrases where this word appears
    public  Bitfield flags;       // the flag bits for each word

    public Word(final int handle, final int pip, final int nop) {
        this.count = 1;
        this.posInText = handle;
        this.posInPhrase = pip;
        this.numOfPhrase = nop;
        this.phrases = new HashSet<Integer>();
        this.flags = null;
    }

    public void inc() {
        this.count++;
    }

    public int occurrences() {
        return this.count;
    }

    public void check(final int i) {
        this.phrases.add(LargeNumberCache.valueOf(i));
    }

    public Iterator<Integer> phrases() {
        // returns an iterator to handles of all phrases where the word appears
        return this.phrases.iterator();
    }

    @Override
    public String toString() {
        // this is here for debugging
        return "{count=" + this.count + ", posInText=" + this.posInText + ", posInPhrase=" + this.posInPhrase + ", numOfPhrase=" + this.numOfPhrase + "}";
    }

    // static methods
    public static byte[] word2hash(final StringBuilder word) {
        return word2hash(word.toString());
    }

    // create a word hash
    public static final byte[] word2hash(final String word) {
    	final String wordlc = word.toLowerCase(Locale.ENGLISH);
    	byte[] h = hashCache.get(wordlc);
        if (h != null) return h;
        // calculate the hash
    	h = Base64Order.enhancedCoder.encodeSubstring(Digest.encodeMD5Raw(wordlc), commonHashLength);
        assert h[2] != '@';
        if (MemoryControl.shortStatus()) {
            hashCache.clear();
        } else {
            //hashCache.putIfAbsent(wordlc, h); // prevent expensive MD5 computation and encoding
            hashCache.insertIfAbsent(wordlc, h); // prevent expensive MD5 computation and encoding
        }
        return h;
    }

    public static final HandleSet words2hashesHandles(final Set<String> words) {
        final HandleSet hashes = new HandleSet(WordReferenceRow.urlEntryRow.primaryKeyLength, WordReferenceRow.urlEntryRow.objectOrder, words.size());
        for (final String word: words)
            try {
                hashes.put(word2hash(word));
            } catch (final RowSpaceExceededException e) {
                Log.logException(e);
                return hashes;
            }
        return hashes;
    }

    public static final HandleSet words2hashesHandles(final String[] words) {
        final HandleSet hashes = new HandleSet(WordReferenceRow.urlEntryRow.primaryKeyLength, WordReferenceRow.urlEntryRow.objectOrder, words.length);
        for (final String word: words)
            try {
                hashes.put(word2hash(word));
            } catch (final RowSpaceExceededException e) {
                Log.logException(e);
                return hashes;
            }
        return hashes;
    }
}
