﻿/***************************************************************************
 *   Copyright (C) 2010 by                                                 *
 *      Itamar Syn-Hershko <itamar at code972 dot com>                     *
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
 ***************************************************************************/

using System;
using System.Collections.Generic;
using System.Text;

namespace HebMorph
{
    public class Tokenizer
    {
        [Flags]
        public enum TokenType : byte
        {
            Hebrew = 1,
            NonHebrew = 2,
            Numeric = 4,
            Construct = 8,
            Acronym = 16,
        }

        public static readonly char[] Geresh = new char[] { '\'' };
        public static readonly char[] Gershayim = new char[] { '\"' };
        public static readonly char[] CharsFollowingPrefixes = new char[] { '"', '\'', '-' };
        public static readonly char[] LettersAcceptingGeresh = new char[] { 'ז', 'ג', 'ץ', 'צ', 'ח' };

        public static bool IsOfChars(char c, char[] options)
        {
            foreach (char o in options)
                if (c == o) return true;
            return false;
        }
        public static bool IsHebrewLetter(char c)
        {
            return (c >= 1488 && c <= 1514);
        }
        public static bool IsNiqqudChar(char c)
        {
            return (c >= 1455 && c <= 1476);
        }

        private System.IO.TextReader input;
        private int dataLen = 0, inputOffset = 0;
        public int Offset { get { return inputOffset; } }

        private const int IO_BUFFER_SIZE = 4096;
        private char[] ioBuffer = new char[IO_BUFFER_SIZE];
        private int ioBufferIndex = 0;

        private char[] wordBuffer = new char[HSpell.Constants.MaxWordLength];

        public Tokenizer(System.IO.TextReader _input)
        {
            this.input = _input;
        }

        // Niqqud is not being removed by design, to allow for a future analyzer extension to take advantage of it
        // This is a job for a normalizer, anyway
        public TokenType NextToken(out string tokenString)
        {
            int length = 0, start = ioBufferIndex;
            TokenType tokenType = 0;
            while (true)
            {
                if (ioBufferIndex >= dataLen)
                {
                    inputOffset += dataLen;
                    dataLen = input.Read((System.Char[])ioBuffer, 0, ioBuffer.Length);
                    if (dataLen <= 0)
                    {
                        dataLen = 0; // so next offset += dataLen won't decrement offset
                        if (length > 0)
                            break;
                        else
                        {
                            tokenString = string.Empty;
                            return 0;
                        }
                    }
                    ioBufferIndex = 0;
                }

                char c = ioBuffer[ioBufferIndex++];
                bool appendCurrentChar = false;

                // In case we already consumed at least one char, and started a non-Hebrew token.
                // Since tokenizing non-Hebrew characters correctly is out of scope for this implementation,
                // we will consume all non-spaces and non-panctuation and return them as-is.
                if (length > 0 && (tokenType & TokenType.NonHebrew) > 0)
                {
                    // No such thing as mixed words; return the current word and go back
                    if ((c >= 1488 && c <= 1514) || (c >= 1455 && c <= 1476)) // HEBREW || NIQQUD
                    {
                        --ioBufferIndex;
                        break;
                    }
                    else if (System.Char.IsLetterOrDigit(c))// TODO: break to prevent mixing of non-Hebrew and digits as well?
                    {
                        appendCurrentChar = true;
                    }
                    else
                        break; // Tokenize on everything else
                }
                else if (IsHebrewLetter(c) || (length > 0 && IsNiqqudChar(c))) // HEBREW || (NIQQUD if not first char)
                {
                    tokenType |= TokenType.Hebrew;
                    appendCurrentChar = true;
                }
                else if (System.Char.IsLetterOrDigit(c))
                {
                    // If met while consuming a Hebrew word, we return the current word (no such thing as mixed words)
                    if (length > 0 && (tokenType & TokenType.Hebrew) > 0)
                    {
                        --ioBufferIndex;
                        break;
                    }

                    tokenType |= TokenType.NonHebrew;
                    if (System.Char.IsDigit(c)) // TODO: break to prevent mixing of non-Hebrew and digits as well?
                        tokenType |= TokenType.Numeric;

                    appendCurrentChar = true;
                }
                else if (c == '"' && length > 0)
                {
                    // Tokenize if previous char wasn't part of a word
                    if (!IsHebrewLetter(wordBuffer[length - 1]) && !IsNiqqudChar(wordBuffer[length - 1]))
                        break;

                    // TODO: Is it possible to support cases like שה"שםעצם in the tokenizer?
                    tokenType |= TokenType.Acronym;
                    appendCurrentChar = true;
                }
                else if (c == '\'' && length > 0)
                {
                    // Tokenize if previous char wasn't part of a word or another Geresh (which we handle below)
                    if (!IsHebrewLetter(wordBuffer[length - 1]) && !IsNiqqudChar(wordBuffer[length - 1])
                        && wordBuffer[length - 1] != '\'')
                        break;

                    // TODO: Is it possible to handle cases which are similar to Merchaot - ה'חלל הפנוי' here?
                    tokenType |= TokenType.Acronym;
                    appendCurrentChar = true;
                }
                else if (length > 0)
                {
                    // Flag makaf connected words as constructs
                    if (c == '-') // TODO: Normalize or support other types of dashes too
                        tokenType |= TokenType.Construct;
                    // TODO: Detect words where Makaf is used for shortening a word (א-ל, י-ם and similar), instead of tokenizing on it

                    // at non-Letter w/ chars
                    break; // return 'em
                }

                if (appendCurrentChar)
                {
                    // Consume normally
                    if (length == 0) // mark the start of a new token
                        start = inputOffset + ioBufferIndex - 1;
                    else if (length == wordBuffer.Length)
                        // buffer overflow!
                        break;

                    // Fix a common replacement of double-Geresh with Gershayim; call it Gershayim normalization if you wish
                    if (c == '\'')
                    {
                        if (wordBuffer[length - 1] == c)
                            wordBuffer[length - 1] = '"';
                        else if (IsOfChars(wordBuffer[length - 1], LettersAcceptingGeresh))
                            wordBuffer[length++] = c;
                    }
                    else
                        wordBuffer[length++] = c; // TODO: Normalize c
                }
            }

            if (wordBuffer[length - 1] == '"')
            {
                wordBuffer[--length] = '\0';
            }
            // Geresh trimming; only try this if it isn't one-char in length (without the Geresh)
            if (length > 2 && wordBuffer[length - 1] == '\'')
            {
                // All letters which this Geresh may mean something for
                if (!IsOfChars(wordBuffer[length - 2], LettersAcceptingGeresh))
                    wordBuffer[--length] = '\0';
                // TODO: Support marking abbrevations (פרופ') and Hebrew's th (ת')
                // TODO: Handle ה (Hashem)
            }

            tokenString = new string(wordBuffer, 0, length);
            return tokenType;
        }

        public void Reset(System.IO.TextReader _input)
        {
            this.input = _input;
            inputOffset = 0;
            dataLen = 0;
            ioBufferIndex = 0;
        }
    }
}
