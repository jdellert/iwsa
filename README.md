# iwsa
Information-Weighted Sequence Alignment (IWSA) for historical linguistics.

The behaviour and usage of all classes with main methods is described here.
The code is largely self-documenting through human-readable names,
errors will usually be easy to trace to an unmet assumption (undefined language codes, wrong file names, etc.)

Main iwsa package (Tools for processing and model inspection)
=============================================================

ConceptLevelEditDistanceOutput <CLDF file>
------------------------------------------
* reads in a file in the NorthEuraLex dump format
* TSV output of all word pairs designating the same concept, along with their normalized edit distance (NED)

ConceptLevelInformationWeightedEditDistanceOutput <CLDF file> (<lang1> ... <langK>)
-----------------------------------------------------------------------------------
* reads in a file in the NorthEuraLex dump format, and a list of language codes to define a subset of the data
* checks whether a global similarity matrix exists at <CLDF file>-global-iw.corr, infers it if not
* checks whether local similarity matrices exist at <CLDF file>-local-iw.corr, infers them for the given languages if not
* TSV output of all word pairs designating the same concept, along with their IWD and IWDSC distances (plus the minimum of the two)
* two boolean flags allow to print out the alignments, and to deactivate local phoneme similarities

ConceptLevelWeightedEditDistanceOutput <CLDF file> (<lang1> ... <langK>)
------------------------------------------------------------------------
* reads in a file in the NorthEuraLex dump format, and a list of language codes to define a subset of the data
* checks whether a global similarity matrix exists at <CLDF file>-global-nw.corr, infers it if not
* checks whether local similarity matrices exist at <CLDF file>-local-nw.corr, infers them for the given languages if not
* TSV output of all word pairs designating the same concept, along with their NWD and NWDSC distances (plus the minimum of the two)
* two boolean flags allow to print out the alignments, and to deactivate local phoneme similarities

CorrespondenceModelOutput <Correspondence file> (<lang1> <lang2>)
-------------------------------------------------------------------------
* reads a global similarity matrix file if language IDs are not given, and local similarity matrices if they are
* prints out the global matrix (or the matrix for the selected pair) in a three-column TSV format

CorrespondenceModelVisualizationLaTeXTable <Correspondence file>
----------------------------------------------------------------
* reads a global similarity matrix file, and visualizes the encoded correspondences as a LaTeX table
* result is LaTeX code consisting of a header and a table
* table can be pasted into any LaTeX document provided the tipa package for IPA is included
* no support for extraction of local similarity scores yet

InformationLengthPerWordOutput <CLDF file> (<lang1> ... <langK>)
----------------------------------------------------------------
* reads in a file in the NorthEuraLex dump format, and a list of language codes to define a subset of the data
* infers information models for all languages in the selected subset
* infers the segment-wise information weight for all segments in word forms in the subset of the database
* adds up the information weights for each word-form, which defines a corrected word length
* TSV output of word forms and their information length

InformationModelOutput <CLDF file> <lang>
-----------------------------------------
* reads in a file in the NorthEuraLex dump format, and a language ID as the second argument
* builds the trigram and gappy bigram models for the language specified by the ID
* prints out the observation counts, sorted by gappy bigram

InformationWeightOutput <CLDF file> <lang>
------------------------------------------
* reads in a file in the NorthEuraLex dump format, and a language ID as the second argument
* builds the trigram and gappy bigram models for the language specified by the ID
* prints all forms for the language with information weights for each segment

InformationWeightOutputLaTeX <CLDF file> <lang>
-----------------------------------------------
* reads in a file in the NorthEuraLex dump format, and a language ID as the second argument
* builds the trigram and gappy bigram models for the language specified by the ID
* prints all forms for the language, with information weights for each segment, into a LaTeX table
* table can be pasted into any LaTeX document provided the tipa package for IPA is included


iwsa.eval package (additional tools for evaluation)
===================================================

AllDistancesForWordPairFile <CLDF file> <gold standard file>
------------------------------------------------------------
* takes the NorthEuraLex CLDF file and the pairwise cognacy gold standard file (cognacy-eval-pairs.tsv)
* prints out the gold standard with additional columns containing all the distances compared in the paper (NED, NWD, NWDSC, IWD, IWDSC)
* LexStat distances need to be added manually as an additional column
* this tool will NOT infer the necessary similarity matrices if they are missing

PrecisionRecallCurveOutput <all distances output file> <leftmost result column index>
-------------------------------------------------------------------------------------
* takes the output file of the previous program (pairwise cognacy gold standard with named distance columns)
* operates on all distances contained in columns after the index
* creates a new file named by each column, consisting of precision-and-recall pairs for the respective distances
* the precision-and-recall pairs can be used for visualizing the curves e.g. in R
* prints out a table detailing the average precision, the optimal F-score and the associated precision and recall values for each distance column

