package de.jdellert.iwsa.tokenize;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class IPATokenizer {
	public final IPATokenizerConfiguration config;

	private Set<Character> ignoredIPASegments;
	private Set<Character> relevantIPASegments;
	private Set<Character> combiningDiacritics;
	private Set<String> multiSegments;

	public IPATokenizer() {
		config = new IPATokenizerConfiguration();
		initialize();
	}

	public IPATokenizer(IPATokenizerConfiguration config) {
		this.config = config;
		initialize();
	}

	private void initialize() {
		ignoredIPASegments = new HashSet<Character>();
		relevantIPASegments = new HashSet<Character>();
		combiningDiacritics = new HashSet<Character>();
		multiSegments = new HashSet<String>();

		ignoredIPASegments.add('~');

		relevantIPASegments.add('_');
		relevantIPASegments.add('a');
		relevantIPASegments.add('b');
		relevantIPASegments.add('c');
		relevantIPASegments.add('d');
		relevantIPASegments.add('e');
		relevantIPASegments.add('f');
		relevantIPASegments.add('h');
		relevantIPASegments.add('i');
		relevantIPASegments.add('j');
		relevantIPASegments.add('k');
		relevantIPASegments.add('l');
		relevantIPASegments.add('m');
		relevantIPASegments.add('n');
		relevantIPASegments.add('o');
		relevantIPASegments.add('p');
		relevantIPASegments.add('q');
		relevantIPASegments.add('r');
		relevantIPASegments.add('s');
		relevantIPASegments.add('t');
		relevantIPASegments.add('u');
		relevantIPASegments.add('v');
		relevantIPASegments.add('w');
		relevantIPASegments.add('x');
		relevantIPASegments.add('y');
		relevantIPASegments.add('z');
		relevantIPASegments.add('æ');
		relevantIPASegments.add('ç');
		relevantIPASegments.add('ð');
		relevantIPASegments.add('ø');
		relevantIPASegments.add('ħ');
		relevantIPASegments.add('ŋ');
		relevantIPASegments.add('œ');
		relevantIPASegments.add('ɐ');
		relevantIPASegments.add('ɑ');
		relevantIPASegments.add('ɒ');
		relevantIPASegments.add('ɔ');
		relevantIPASegments.add('ɕ');
		relevantIPASegments.add('ɖ');
		relevantIPASegments.add('ɘ');
		relevantIPASegments.add('ə');
		relevantIPASegments.add('ɛ');
		relevantIPASegments.add('ɜ');
		relevantIPASegments.add('ɟ');
		relevantIPASegments.add('ɡ');
		relevantIPASegments.add('ɢ');
		relevantIPASegments.add('ɣ');
		relevantIPASegments.add('ɤ');
		relevantIPASegments.add('ɥ');
		relevantIPASegments.add('ɦ');
		relevantIPASegments.add('ɨ');
		relevantIPASegments.add('ɪ');
		relevantIPASegments.add('ɫ');
		relevantIPASegments.add('ɬ');
		relevantIPASegments.add('ɭ');
		relevantIPASegments.add('ɮ');
		relevantIPASegments.add('ɯ');
		relevantIPASegments.add('ɱ');
		relevantIPASegments.add('ɲ');
		relevantIPASegments.add('ɳ');
		relevantIPASegments.add('ɴ');
		relevantIPASegments.add('ɵ');
		relevantIPASegments.add('ɸ');
		relevantIPASegments.add('ɹ');
		relevantIPASegments.add('ɺ');
		relevantIPASegments.add('ɻ');
		relevantIPASegments.add('ɽ');
		relevantIPASegments.add('ɾ');
		relevantIPASegments.add('ʀ');
		relevantIPASegments.add('ʁ');
		relevantIPASegments.add('ʂ');
		relevantIPASegments.add('ʃ');
		relevantIPASegments.add('ʈ');
		relevantIPASegments.add('ʉ');
		relevantIPASegments.add('ʊ');
		relevantIPASegments.add('ʋ');
		relevantIPASegments.add('ʌ');
		relevantIPASegments.add('ʍ');
		relevantIPASegments.add('ʎ');
		relevantIPASegments.add('ʏ');
		relevantIPASegments.add('ʐ');
		relevantIPASegments.add('ʑ');
		relevantIPASegments.add('ʒ');
		relevantIPASegments.add('ʔ');
		relevantIPASegments.add('ʕ');
		relevantIPASegments.add('ʜ');
		relevantIPASegments.add('ʝ');
		relevantIPASegments.add('ʡ');
		relevantIPASegments.add('ʰ');
		relevantIPASegments.add('ʲ');
		relevantIPASegments.add('ʷ');
		relevantIPASegments.add('ʼ');
		relevantIPASegments.add('ˀ');
		relevantIPASegments.add('ˠ');
		relevantIPASegments.add('ˤ');
		relevantIPASegments.add('̃');
		relevantIPASegments.add('β');
		relevantIPASegments.add('θ');
		relevantIPASegments.add('χ');

		combiningDiacritics.add('͡');
		combiningDiacritics.add('ː');

		if (!config.SEPARATE_EJECTIVITY) {
			combiningDiacritics.add('ʼ');
		}
		if (!config.SEPARATE_LABIALIZATION) {
			combiningDiacritics.add('ʷ');
		}
		if (!config.SEPARATE_PALATALIZATION) {
			combiningDiacritics.add('ʲ');
		}
		if (!config.SEPARATE_ASPIRATION) {
			combiningDiacritics.add('ʰ');
		}
		if (!config.SEPARATE_VELARIZATION) {
			combiningDiacritics.add('ˠ');
			combiningDiacritics.add('̴');
		}
		if (!config.SEPARATE_GLOTTALIZATION) {
			combiningDiacritics.add('ˀ');
		}
		if (!config.SEPARATE_PHARYNGEALIZATION) {
			combiningDiacritics.add('ˤ');
		}
		if (!config.SEPARATE_NASAL_RELEASE) {
			combiningDiacritics.add('ⁿ');
		}
		if (!config.SEPARATE_LATERAL_RELEASE) {
			combiningDiacritics.add('ˡ');
		}
		if (!config.SEPARATE_NASALIZATION) {
			combiningDiacritics.add('̃');
		}

		if (config.SINGLE_SEGMENT_AFFRICATES) {
			multiSegments.add("c͡ç");
			multiSegments.add("d͡z");
			multiSegments.add("d͡ʑ");
			multiSegments.add("d͡ʒ");
			multiSegments.add("p͡f");
			multiSegments.add("q͡χ");
			multiSegments.add("q͡χˤ");
			multiSegments.add("t͡s");
			multiSegments.add("t͡sʼ");
			multiSegments.add("t͡ɕ");
			multiSegments.add("t͡ɬ");
			multiSegments.add("t͡ɬʼ");
			multiSegments.add("t͡ɬʼˤ");
			multiSegments.add("t͡ɬˤ");
			multiSegments.add("ʈ͡ʂ");
			multiSegments.add("ʈ͡ʂʼ");
			multiSegments.add("t͡ʃ");
			multiSegments.add("t͡ʃʼ");
			multiSegments.add("t͡ʃʼˤ");
			multiSegments.add("t͡ʃˤ");
			multiSegments.add("ɖ͡ʐ");
			multiSegments.add("ɟ͡ʝ");
		}
	}

	/**
	 * Tokenizes an ipaString according to the configuration defined during
	 * construction.
	 * 
	 * @param ipaString
	 *            a unicode IPA string, possible pre-tokenized by spaces
	 * @return an array of strings, each representing a phonetic segment
	 */
	public String[] tokenizeIPA(String ipaString) {
		List<String> segments = new LinkedList<String>();

		boolean coarticulation = false;

		StringBuilder currentSegment = new StringBuilder();
		for (char c : ipaString.toCharArray()) {
			if (ignoredIPASegments.contains(c))
				continue;
			if (combiningDiacritics.contains(c)) {
				if (c == '͡') {
					coarticulation = true;
				} else if (c == 'ː' || c == ':') {
					if (currentSegment.toString().equals("̃")) {
						segments.add(segments.get(segments.size() - 1));
						segments.add("̃");
					} else {
						if (currentSegment.length() == 0 && segments.size() > 0) {
							// treatment of length signs after affricates: C)c: -> CC)c
							String lastSegment = segments.remove(segments.size() - 1);
							segments.add(lastSegment.substring(0, 1));
							segments.add(lastSegment);
						} else {
							segments.add(currentSegment.toString());
							segments.add(currentSegment.toString());
						}
					}
					currentSegment = new StringBuilder();
				} else {
					currentSegment.append(c);
				}
			} else {
				if (coarticulation) {
					// ignore coarticulation except for predefined set of multi-segments
					coarticulation = false;
					if (multiSegments.contains(currentSegment.toString() + '͡' + c)) {
						currentSegment.append('͡');
						currentSegment.append(c);
						segments.add(currentSegment.toString());
						currentSegment = new StringBuilder();
					} else {
						if (currentSegment.length() > 0)
							segments.add(currentSegment.toString());
						currentSegment = new StringBuilder();
						if (!config.IGNORE_UNKNOWN_SYMBOLS || relevantIPASegments.contains(c))
							currentSegment.append(c);
					}
				} else {
					if (currentSegment.length() > 0)
						segments.add(currentSegment.toString());
					currentSegment = new StringBuilder();
					if (c == ' ') {
						currentSegment.append('_');
					} else {
						if (!config.IGNORE_UNKNOWN_SYMBOLS || relevantIPASegments.contains(c))
							currentSegment.append(c);
					}
				}
			}
		}
		if (currentSegment.length() > 0)
			segments.add(currentSegment.toString());
		return segments.toArray(new String[segments.size()]);
	}
}
