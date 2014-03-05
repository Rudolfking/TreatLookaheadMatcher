package hu.bme.mit.inf.lookaheadmatcher.impl;

import java.util.Random;

public class RandomPrefixesGenerator
{
	private static String[] randoms = new String[]{"Timi","Balazs","Pozsony","Bratislava","Zlaty_Bazant","UFO","Alberlet","Csillagpark","Dobogoko","Kirandulas"};
	private static int currentIndex = 0;
	public static Random R = new Random(12256);
	public static String get()
	{
		if (currentIndex >= randoms.length || currentIndex < 0)
			currentIndex = 0;
		return randoms[currentIndex++];
	}
}
