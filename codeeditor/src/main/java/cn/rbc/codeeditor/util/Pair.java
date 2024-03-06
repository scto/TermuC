/*
 * Copyright (c) 2013 Tah Wei Hoon.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Apache License Version 2.0,
 * with full text available at http://www.apache.org/licenses/LICENSE-2.0.html
 *
 * This software is provided "as is". Use at your own risk.
 */
package cn.rbc.codeeditor.util;

public final class Pair {
	public int first;
	public int second;
	
	public Pair(int x, int y){
		first = x;
		second = y;
	}

    @Override
    public String toString() {
        return "("+first+","+second+")";
    }
    
}
