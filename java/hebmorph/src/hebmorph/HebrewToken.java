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

import hebmorph.hspell.LingInfo;

public class HebrewToken extends Token implements Comparable<Token>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5809495040446607703L;

	public HebrewToken(String _word, int _prefixLength, Integer _mask, String _lemma, float _score)
	{
		super(_word);
		prefixLength = _prefixLength;
		setMask(_mask);
		if (_lemma == null)
		{
			lemma = _word.substring(prefixLength); // Support null lemmas while still taking into account prefixes
		}
		else
		{
			lemma = _lemma;
		}
		setScore(_score);
	}

	private float score = 1.0f;
	private int prefixLength;
	private Integer mask;
	private String lemma;

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((lemma == null) ? 0 : lemma.hashCode());
		result = prime * result + ((mask == null) ? 0 : mask.hashCode());
		result = prime * result + prefixLength;
		result = prime * result + Float.floatToIntBits(score);
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof HebrewToken)) {
			return false;
		}
		HebrewToken other = (HebrewToken) obj;
		if (lemma == null) {
			if (other.lemma != null) {
				return false;
			}
		} else if (!lemma.equals(other.lemma)) {
			return false;
		}
		if (mask == null) {
			if (other.mask != null) {
				return false;
			}
		} else if (!mask.equals(other.mask)) {
			return false;
		}
		if (prefixLength != other.prefixLength) {
			return false;
		}
		if (Float.floatToIntBits(score) != Float.floatToIntBits(other.score)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString()
	{
		return String.format("(%s) %s", LingInfo.DMask2EnglishString(getMask()), lemma);
	}

	public final int compareTo(Token token)
	{
		HebrewToken other = (HebrewToken)((token instanceof HebrewToken) ? token : null);
        if (other == null) return -1;

        return ((Float)getScore()).compareTo(other.getScore());
	}

	public void setScore(float score)
	{
		this.score = score;
	}

	public float getScore()
	{
		return score;
	}

	public void setMask(Integer mask)
	{
		this.mask = mask;
	}

	public Integer getMask()
	{
		return mask;
	}

	public int getPrefixLength()
	{
		return prefixLength;
	}

	public String getLemma()
	{
		return lemma;
	}


}