/**************************************************************************
 *   Copyright (C) 2010 by                                                 *
 *      Itamar Syn-Hershko <itamar at code972 dot com>                     *
 *		Ofer Fort <oferiko at gmail dot com>							   *
 *                                                                         *
 *   Distributed under the GNU General Public License, Version 2.0.        *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation (v2).                                    *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Steet, Fifth Floor, Boston, MA  02111-1307, USA.          *
 **************************************************************************/
package hebmorph;
 
import hebmorph.datastructures.DictRadix;
import hebmorph.datastructures.RealSortedList;
import hebmorph.datastructures.RealSortedList.SortOrder;
import hebmorph.hspell.LingInfo;
import hebmorph.hspell.Loader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.search.spell.Dictionary;

public class Lemmatizer implements Dictionary
{
	
	static String[] similarTokens = {"העא", "גה", "כח", "תט", "צס", "שס",
		  "כק", "בו", "פב","זס"};
	
	private DictRadix<MorphData> m_dict;
	private DictRadix<Integer> m_prefixes;
	private boolean m_IsInitialized = false;
	
	public Lemmatizer()
	{
	}
	
	public boolean getIsInitialized()
	{
		return m_IsInitialized;
	}

	public Lemmatizer(String hspellPath, boolean loadMorpholicData, boolean allowHeHasheela) throws IOException
	{
		initFromHSpellFolder(hspellPath, loadMorpholicData, allowHeHasheela);
	}

	public void initFromHSpellFolder(String path, boolean loadMorpholicData, boolean allowHeHasheela) throws IOException
	{
		m_dict = Loader.loadDictionaryFromHSpellFolder(path, loadMorpholicData);
		m_prefixes = LingInfo.buildPrefixTree(allowHeHasheela);
		m_IsInitialized = true;
	}
	
	public Lemmatizer(ClassLoader classLoader,String packagePath, boolean loadMorpholicData, boolean allowHeHasheela) throws IOException
	{
		initFromHSpellFolder(classLoader,packagePath, loadMorpholicData, allowHeHasheela);
	}
	
	public void initFromHSpellFolder(ClassLoader classLoader,String packagePath, boolean loadMorpholicData, boolean allowHeHasheela) throws IOException {
		m_dict = Loader.loadDictionaryFromHSpellFolder(classLoader,packagePath, loadMorpholicData);
		m_prefixes = LingInfo.buildPrefixTree(allowHeHasheela);
		m_IsInitialized = true;
	}
	
	public boolean addCustomRelation(String existWord,String newWord,int newWordMask ) throws NoSuchFieldException {

		if( m_dict.lookup(newWord)!=null ) {
			return false;
		}
		
		MorphData existData = m_dict.lookup(existWord);
		if( existData==null) {
			throw new NoSuchFieldException(existWord+" not found");
		}
			

		MorphData newData = new MorphData();
		
		Integer[] existFlags = new Integer[existData.getDescFlags().length+1];
		Integer[] newFlags = new Integer[existData.getDescFlags().length+1];
		System.arraycopy(existData.getDescFlags(), 0, existFlags, 0, existData.getDescFlags().length);
		System.arraycopy(existData.getDescFlags(), 0, newFlags, 0, existData.getDescFlags().length); 
		existFlags[existData.getDescFlags().length]=0;
		newFlags[existData.getDescFlags().length]=existData.getDescFlags()[0];
		
		existData.setDescFlags(existFlags);
		newData.setDescFlags(newFlags);
		
		String[] existLemmas = new String[existData.getLemmas().length+1];
		String[] newLemmas = new String[existData.getLemmas().length+1];
		System.arraycopy(existData.getLemmas(), 0, existLemmas, 0, existData.getLemmas().length);
		System.arraycopy(existData.getLemmas(), 0, newLemmas, 0, existData.getLemmas().length); 
		existLemmas[existData.getLemmas().length] = newWord;
		newLemmas[existData.getLemmas().length] = existWord;
		
		
		existData.setLemmas(existLemmas);
		newData.setLemmas(newLemmas);
		
		m_dict.addNode(newWord,newData);
		
		return true;
	}
	
	public boolean isLegalPrefix(String str)
	{
		Integer lookup = m_prefixes.lookup(str);
		if ((lookup!=null) && (lookup > 0))
		{
			return true;
		}

		return false;
	}

	// See the Academy's punctuation rules (see לשוננו לעם, טבת, תשס"ב) for an explanation of this rule
	public String tryStrippingPrefix(String word)
	{
		// TODO: Make sure we conform to the academy rules as closely as possible

		int firstQuote = word.indexOf('"');

		if (firstQuote > -1)
		{
			if (isLegalPrefix(word.substring(0, firstQuote)))
			{
				return word.substring(firstQuote + 1, firstQuote + 1 + word.length() - firstQuote - 1);
			}
		}

		int firstSingleQuote = word.indexOf('\'');
		if (firstSingleQuote == -1)
		{
			return word;
		}

		if ((firstQuote > -1) && (firstSingleQuote > firstQuote))
		{
			return word;
		}

		if (isLegalPrefix(word.substring(0, firstSingleQuote)))
		{
			return word.substring(firstSingleQuote + 1, firstSingleQuote + 1 + word.length() - firstSingleQuote - 1);
		}

		return word;
	}

	/**
	 Removes all Niqqud character from a word

	 @param word A string to remove Niqqud from
	 @return A new word "clean" of Niqqud chars
	*/
	public static String removeNiqqud(String word)
	{
		int length = word.length();
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++)
		{
			if ((word.charAt(i) < 1455) || (word.charAt(i) > 1476)) // current position is not a Niqqud character
			{
				sb.append(word.charAt(i));
			}
		}
		return sb.toString();
	}
	
	/**
	 * similar to lemmatize(word) method, that only check the ability,
	 * usefull and used for spell checking in  tryCorrect
	 * @param word
	 * @return
	 */
	public boolean haveLemmas(String word) {
		MorphData md = m_dict.lookup(word);
		if (md != null)
		{
			return true;
		}
		else if (word.endsWith("'")) // Try ommitting closing Geresh
		{
			md = m_dict.lookup(word.substring(0, word.length() - 1));
			if (md != null)
			{
				return true;
			}
		}

		int prefLen = 0;
		Integer prefixMask;
		while (true)
		{
			// Make sure there are at least 2 letters left after the prefix (the words של, שלא for example)
			if (word.length() - prefLen < 2)
			{
				break;
			}

			prefixMask = m_prefixes.lookup(word.substring(0, ++prefLen));
			if ((prefixMask== null) ||  (prefixMask== 0)) // no such prefix
			{
				break;
			}

			md = m_dict.lookup(word.substring(prefLen));
			if ((md != null) && ((md.getPrefixes() & prefixMask) > 0))
			{
				return true;
			}
		}

		
		return false;
	}

	public List<HebrewToken> lemmatize(String word)
	{
		// TODO: Verify word to be non-empty and contain Hebrew characters?

		RealSortedList<HebrewToken> ret = new RealSortedList<HebrewToken>(SortOrder.Desc);

		MorphData md = m_dict.lookup(word);
		if (md != null)
		{
			for (int result = 0; result < md.getLemmas().length; result++)
			{
				ret.addUnique(new HebrewToken(word, 0, md.getDescFlags()[result], md.getLemmas()[result], 1.0f));
			}
		}
		else if (word.endsWith("'")) // Try ommitting closing Geresh
		{
			md = m_dict.lookup(word.substring(0, word.length() - 1));
			if (md != null)
			{
				for (int result = 0; result < md.getLemmas().length; result++)
				{
					ret.addUnique(new HebrewToken(word, 0, md.getDescFlags()[result], md.getLemmas()[result], 1.0f));
				}
			}
		}

		int prefLen = 0;
		Integer prefixMask;
		while (true)
		{
			// Make sure there are at least 2 letters left after the prefix (the words של, שלא for example)
			if (word.length() - prefLen < 2)
			{
				break;
			}

			prefixMask = m_prefixes.lookup(word.substring(0, ++prefLen));
			if ((prefixMask== null) ||  (prefixMask== 0)) // no such prefix
			{
				break;
			}

			md = m_dict.lookup(word.substring(prefLen));
			if ((md != null) && ((md.getPrefixes() & prefixMask) > 0))
			{
				for (int result = 0; result < md.getLemmas().length; result++)
				{
					if ((LingInfo.DMask2ps(md.getDescFlags()[result]) & prefixMask) > 0)
					{
						ret.addUnique(new HebrewToken(word, prefLen, md.getDescFlags()[result], md.getLemmas()[result], 0.9f));
					}
				}
			}
		}

		if (ret.size() > 0)
		{
			return ret;
		}
		return null;
	}

	public List<HebrewToken> lemmatizeTolerant(String word)
	{
		// TODO: Verify word to be non-empty and contain Hebrew characters?

		RealSortedList<HebrewToken> ret = new RealSortedList<HebrewToken>(SortOrder.Desc);

		int prefLen = 0;
		Integer prefixMask;

		List<DictRadix<MorphData>.LookupResult> tolerated = m_dict.lookupTolerant(word, LookupTolerators.TolerateEmKryiaAll);
		if (tolerated != null)
		{
			for (DictRadix<MorphData>.LookupResult lr : tolerated)
			{
				for (int result = 0; result < lr.getData().getLemmas().length; result++)
				{
					ret.addUnique(new HebrewToken(lr.getWord(), 0, lr.getData().getDescFlags()[result], lr.getData().getLemmas()[result], lr.getScore()));
				}
			}
		}

		prefLen = 0;
		while (true)
		{
			// Make sure there are at least 2 letters left after the prefix (the words של, שלא for example)
			if (word.length() - prefLen < 2)
			{
				break;
			}

			prefixMask = m_prefixes.lookup(word.substring(0, ++prefLen));
			if ((prefixMask ==null) || (prefixMask == 0)) // no such prefix
			{
				break;
			}

			tolerated = m_dict.lookupTolerant(word.substring(prefLen), LookupTolerators.TolerateEmKryiaAll);
			if (tolerated != null)
			{
				for (DictRadix<MorphData>.LookupResult lr : tolerated)
				{
					for (int result = 0; result < lr.getData().getLemmas().length; result++)
					{
						if ((LingInfo.DMask2ps(lr.getData().getDescFlags()[result]) & prefixMask) > 0)
						{
							ret.addUnique(new HebrewToken(word.substring(0, prefLen) + lr.getWord(), prefLen, lr.getData().getDescFlags()[result], lr.getData().getLemmas()[result], lr.getScore() * 0.9f));
						}
					}
				}
			}
		}

		if (ret.size() > 0)
		{
			return ret;
		}
		return null;
	}
	
	
	private static String splice(String word,int prefixEnd,int sufixStart,char... middleChars ) {
		StringBuilder builder = new StringBuilder();
		builder.append(word.substring(0,prefixEnd));
		for (char c : middleChars) {
			builder.append(c);
		}
		builder.append(word.substring(sufixStart));
		
		return builder.toString();
	}
	
	/**
	 * @param word
	 * @return
	 */
	public List<String> tryCorrect(String word)
	{
		List<String> corrections = new ArrayList<String>();
		String potentialWord;
		int i;
		int len=word.length();
		

	
//		 try to add a missing em kri'a - yud or vav 
		for(i=1;i<len;i++){
			potentialWord = splice(word,i,i,'י');
			if( haveLemmas(potentialWord) ) corrections.add(potentialWord);
			
			potentialWord = splice(word,i,i,'ו');
			if( haveLemmas(potentialWord) ) corrections.add(potentialWord);
		}
//		 try to remove an em kri'a - yud or vav 
//		 NOTE: in hspell.pl the loop was from i=0 to i<len... 
		for(i=1;i<len-1;i++){
			if(word.charAt(i)=='י' || word.charAt(i)=='ו'){
				potentialWord = splice(word,i,i+1);
				if( haveLemmas(potentialWord) ) corrections.add(potentialWord);
			}
		}
		/* try to add or remove an aleph (is that useful?) 
		 TODO: don't add an aleph next to yud or non-double vav,
		 * as it can't be an em kria there? 
		 */		
		for(i=1;i<len;i++){
			potentialWord = splice(word,i,i,'א');
			if( haveLemmas(potentialWord) ) corrections.add(potentialWord);
		}
		for(i=1;i<len-1;i++){
			if(word.charAt(i)=='א'){
				potentialWord = splice(word,i,i+1);
				if( haveLemmas(potentialWord) ) corrections.add(potentialWord);
			}
		}
//		 try to replace similarly sounding (for certain people) letters:
		 
		for(i=0;i<len;i++){
			
			for (String  simToken : similarTokens) {
				int j;
				for ( j = 0; j<simToken.length() && word.charAt(i)!=simToken.charAt(j); j++);
//				for(g=similar[group];*g && *g!=w[i];g++);
					
				if( j<simToken.length() ){
					/* character in group - try the other ones
					 * in this group! */
					char[] another = simToken.toCharArray();
					for (int k = 0; k < another.length; k++) {
						
						if(another[k]==word.charAt(i)) continue;
						
						if( i+1<len && word.charAt(i)=='ו' && word.charAt(i+1)=='ו')
							potentialWord = splice(word,i,i+2,another[k]);
						else if(another[k]=='ו')
							potentialWord = splice(word,i,i+1,another[k],'ו','ו');
						else
							potentialWord = splice(word,i,i+1,another[k]);
						if( haveLemmas(potentialWord) ) corrections.add(potentialWord);
					}
				}
			}
		}
		/* try to replace a non-final letter at the end of the word by its
		 * final form and vice versa (useful check for abbreviations) */
//		strncpy(buf,w,sizeof(buf));
		StringBuilder buf = new StringBuilder(word);
		switch(word.charAt(len-1)){
			case 'ך': buf.replace(len-1,len, "כ"); break;
			case 'ם': buf.replace(len-1,len, "מ"); break;
			case 'ן': buf.replace(len-1,len,"נ"); break;
			case 'ץ': buf.replace(len-1,len,"צ"); break;
			case 'ף': buf.replace(len-1,len,"פ"); break;
			case 'כ': buf.replace(len-1,len,"ך"); break;
			case 'מ': buf.replace(len-1,len,"ם"); break;
			case 'נ': buf.replace(len-1,len,"ן"); break;
			case 'צ': buf.replace(len-1,len,"ץ"); break;
			case 'פ': buf.replace(len-1,len,"ף"); break;
		}
		if(buf.charAt(len-1)!=word.charAt(len-1)){ 
			potentialWord = buf.toString();
			if( haveLemmas(potentialWord) ) corrections.add(potentialWord);
		}
//		 try to make the word into an acronym (add " before last character 
		if(len>=2){
			potentialWord = splice(word,len-1,len,'"',word.charAt(len-1));
//			splice(buf,sizeof(buf),w,len-1,'"',w[len-1],0);
			if( haveLemmas(potentialWord) ) corrections.add(potentialWord);
		}
//		 try to make the word into an abbreviation (add ' at the end) 
//		snprintf(buf,sizeof(buf), "%s'",w);
		potentialWord = word+"'";
		if( haveLemmas(potentialWord) ) corrections.add(potentialWord);
		
		return corrections;
	}
	
	public class WordIterator implements Iterator<String>{
		
		DictRadix<MorphData>.RadixEnumerator wrappedItrator;
		
		WordIterator() {
			wrappedItrator = (DictRadix<MorphData>.RadixEnumerator)m_dict.iterator();
		}

		public boolean hasNext() {
			return wrappedItrator.hasNext();
		}

		public String next() {
			/*MorphData morphData = */wrappedItrator.next();
			return wrappedItrator.getCurrentKey();
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public Iterator<String> getWordsIterator() {
		return new WordIterator();
	}

	public Iterator<MorphData> getMorphIterator() {
		return m_dict.iterator();
	}
	
	public synchronized void close() {
		m_dict.clear();
		m_prefixes.clear();
		m_IsInitialized = false;
	}
}