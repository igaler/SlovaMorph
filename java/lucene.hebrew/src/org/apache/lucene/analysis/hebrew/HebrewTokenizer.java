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
package org.apache.lucene.analysis.hebrew;

import hebmorph.Reference;
import java.io.IOException;
import java.io.Reader;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;


/**
 Tokenizes a given stream using HebMorph's Tokenizer, removes prefixes where possible, and tags Tokens
 with appropriate types where possible

*/
public class HebrewTokenizer extends Tokenizer
{
	private hebmorph.Tokenizer hebMorphTokenizer;
	private hebmorph.datastructures.DictRadix<Integer> prefixesTree;

	private TermAttribute termAtt;
	private OffsetAttribute offsetAtt;
	//private PositionIncrementAttribute posIncrAtt;
	private TypeAttribute typeAtt;

	public HebrewTokenizer(Reader _input)//: base(input) <- converts to CharStream, and causes issues due to a call to ReadToEnd in ctor
	{
		init(_input, hebmorph.hspell.LingInfo.buildPrefixTree(false));
	}

	public HebrewTokenizer(Reader _input, hebmorph.datastructures.DictRadix<Integer> _prefixesTree)//: base(input) <- converts to CharStream, and causes issues due to a call to ReadToEnd in ctor
	{
		init(_input, _prefixesTree);
	}

	private void init(Reader _input, hebmorph.datastructures.DictRadix<Integer> _prefixesTree)
	{
		termAtt = (TermAttribute)addAttribute(TermAttribute.class);
		offsetAtt = (OffsetAttribute)addAttribute(OffsetAttribute.class);
		//posIncrAtt = (PositionIncrementAttribute)AddAttribute(typeof(PositionIncrementAttribute));
		typeAtt = (TypeAttribute)addAttribute(TypeAttribute.class);
		hebMorphTokenizer = new hebmorph.Tokenizer(_input);
		prefixesTree = _prefixesTree;
	}

	public static class TokenTypes
	{
		public static final int Hebrew = 0;
		public static final int NonHebrew = 1;
		public static final int Numeric = 2;
		public static final int Construct = 3;
		public static final int Acronym = 4;
	}

	public static final String[] TOKEN_TYPE_SIGNATURES = new String[]
    {
		"<HEBREW>",
		"<NON_HEBREW>",
		"<NUM>",
		"<CONSTRUCT>",
		"<ACRONYM>",
		null
	};

	public static String tokenTypeSignature(int tokenType)
	{
		return TOKEN_TYPE_SIGNATURES[tokenType];
	}

	@Override
	public boolean incrementToken() throws IOException
	{
		clearAttributes();
//		int start = hebMorphTokenizer.getOffset();

		Reference<String> nextToken = new Reference<String>(null);
        String nextTokenVal = null;
		int tokenType;

		// Used to loop over certain noise cases
		while (true)
		{
			tokenType = hebMorphTokenizer.nextToken(nextToken);
            nextTokenVal = nextToken.ref;

			if (tokenType == 0)
			{
				return false; // EOS
			}

			// Ignore "words" which are actually only prefixes in a single word.
			// This first case is easy to spot, since the prefix and the following word will be
			// separated by a dash marked as a construct (סמיכות) by the Tokenizer
			if ((tokenType & hebmorph.Tokenizer.TokenType.Construct) > 0)
			{
				if (isLegalPrefix(nextToken.ref))
				{
					continue;
				}
			}

			// This second case is a bit more complex. We take a risk of splitting a valid acronym or
			// abbrevated word into two, so we send it to an external function to analyze the word, and
			// get a possibly corrected word. Examples for words we expect to simplify by this operation
			// are ה"שטיח", ש"המידע.
			if ((tokenType & hebmorph.Tokenizer.TokenType.Acronym) > 0)
			{
				nextTokenVal = nextToken.ref = tryStrippingPrefix(nextToken.ref);

				// Re-detect acronym, in case it was a false positive
				if (nextTokenVal.indexOf('"') == -1)
				{
					tokenType |= ~hebmorph.Tokenizer.TokenType.Acronym;
				}
			}

			break;
		}

		// Record the term string
		if (termAtt.termLength() < nextTokenVal.length())
		{
			termAtt.setTermBuffer(nextTokenVal);
		}
		else // Perform a copy to save on memory operations
		{
	        char[] chars = nextTokenVal.toCharArray();
            termAtt.setTermBuffer(chars,0,chars.length);
			//char[] buf = termAtt.termBuffer();
			//nextToken.CopyTo(0, buf, 0, nextToken.length());
		}
		termAtt.setTermLength(nextTokenVal.length());

		offsetAtt.setOffset(correctOffset(hebMorphTokenizer.getOffset()), correctOffset(hebMorphTokenizer.getOffset() + hebMorphTokenizer.getLengthInSource()));

		if ((tokenType & hebmorph.Tokenizer.TokenType.Hebrew) > 0)
		{
			if ((tokenType & hebmorph.Tokenizer.TokenType.Acronym) > 0)
			{
				typeAtt.setType(tokenTypeSignature(TokenTypes.Acronym));
			}
			if ((tokenType & hebmorph.Tokenizer.TokenType.Construct) > 0)
			{
				typeAtt.setType(tokenTypeSignature(TokenTypes.Construct));
			}
			else
			{
				typeAtt.setType(tokenTypeSignature(TokenTypes.Hebrew));
			}
		}
		else if ((tokenType & hebmorph.Tokenizer.TokenType.Numeric) > 0)
		{
			typeAtt.setType(tokenTypeSignature(TokenTypes.Numeric));
		}
		else
		{
			typeAtt.setType(tokenTypeSignature(TokenTypes.NonHebrew));
		}

		return true;
	}

	@Override
	public void end()
	{
		// set final offset
		int finalOffset = correctOffset(hebMorphTokenizer.getOffset());
		offsetAtt.setOffset(finalOffset, finalOffset);
	}

	@Override
	public void reset(Reader input) throws IOException
	{
		super.reset(input);
		hebMorphTokenizer.reset(input);
	}

	public boolean isLegalPrefix(String str)
	{
        Integer val = prefixesTree.lookup(str);
		if (val != null && val > 0)
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
}