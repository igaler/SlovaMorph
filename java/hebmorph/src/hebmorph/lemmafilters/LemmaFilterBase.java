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
package hebmorph.lemmafilters;

import hebmorph.Token;
import java.util.ArrayList;
import java.util.List;


public abstract class LemmaFilterBase
{
	public List<Token> filterCollection(List<Token> collection)
	{
		return filterCollection(collection, null);
	}

	public List<Token> filterCollection(List<Token> collection, List<Token> preallocatedOut)
	{
		if (preallocatedOut == null)
		{
			preallocatedOut = new ArrayList<Token>();
		}
		else
		{
			preallocatedOut.clear();
		}

		for (Token t : collection)
		{
			if (isValidToken(t))
			{
				preallocatedOut.add(t);
			}
		}

		return preallocatedOut;
	}

	public abstract boolean isValidToken(Token t);
}