/*******************************************************************************
 * Copyright 2012
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.tsv;

import static java.util.Arrays.asList;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.runPipeline;
import static org.apache.uima.fit.util.JCasUtil.select;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.JCasFactory;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.testing.factory.TokenBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.junit.Rule;
import org.junit.Test;

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS;
import de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData;
import de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence;
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token;
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.dependency.Dependency;
import de.tudarmstadt.ukp.dkpro.core.testing.DkproTestContext;

public class WebAnnoTsv3ReaderWriterTest
{
    @Test
    public void test()
        throws Exception
    {
        String targetFolder = "target/test-output/" + testContext.getTestOutputFolderName();
        
		CollectionReader reader = CollectionReaderFactory.createReader(
		        WebannoTsv3Reader.class,
		        WebannoTsv3Reader.PARAM_SOURCE_LOCATION, "src/test/resources/tsv3/", 
		        WebannoTsv3Reader.PARAM_PATTERNS, "coref.tsv");

		List<String> slotFeatures = new ArrayList<String>();
		List<String> slotTargets = new ArrayList<String>();
		List<String> linkTypes = new ArrayList<String>();
		List<String> spanLayers = new ArrayList<String>();
		spanLayers.add(NamedEntity.class.getName());
		spanLayers.add(POS.class.getName());
        spanLayers.add(Lemma.class.getName());
		List<String> chainLayers = new ArrayList<String>();
		chainLayers.add("de.tudarmstadt.ukp.dkpro.core.api.coref.type.Coreference");
		List<String> relationLayers = new ArrayList<String>();
		relationLayers.add(Dependency.class.getName());

		AnalysisEngineDescription writer = createEngineDescription(
		        WebannoTsv3Writer.class,
		        WebannoTsv3Writer.PARAM_TARGET_LOCATION, targetFolder,
		        WebannoTsv3Writer.PARAM_STRIP_EXTENSION, true, 
		        WebannoTsv3Writer.PARAM_SPAN_LAYERS, spanLayers, 
		        WebannoTsv3Writer.PARAM_SLOT_FEATS, slotFeatures, 
		        WebannoTsv3Writer.PARAM_SLOT_TARGETS, slotTargets, 
		        WebannoTsv3Writer.PARAM_LINK_TYPES, linkTypes, 
		        WebannoTsv3Writer.PARAM_CHAIN_LAYERS, chainLayers,
		        WebannoTsv3Writer.PARAM_RELATION_LAYERS, relationLayers);

		runPipeline(reader, writer);

		CollectionReader reader1 = CollectionReaderFactory.createReader(WebannoTsv3Reader.class,
		        WebannoTsv3Reader.PARAM_SOURCE_LOCATION, "src/test/resources/tsv3/", 
		        WebannoTsv3Reader.PARAM_PATTERNS, "coref.tsv");

		CollectionReader reader2 = CollectionReaderFactory.createReader(WebannoTsv3Reader.class,
		        WebannoTsv3Reader.PARAM_SOURCE_LOCATION, targetFolder, 
		        WebannoTsv3Reader.PARAM_PATTERNS, "coref.tsv");

		CAS cas1 = JCasFactory.createJCas().getCas();
		reader1.getNext(cas1);

		CAS cas2 = JCasFactory.createJCas().getCas();
		reader2.getNext(cas2);

		assertEquals(JCasUtil.select(cas2.getJCas(), Token.class).size(),
				JCasUtil.select(cas1.getJCas(), Token.class).size());
		assertEquals(JCasUtil.select(cas2.getJCas(), POS.class).size(),
				JCasUtil.select(cas1.getJCas(), POS.class).size());
		assertEquals(JCasUtil.select(cas2.getJCas(), Lemma.class).size(),
				JCasUtil.select(cas1.getJCas(), Lemma.class).size());
		assertEquals(JCasUtil.select(cas2.getJCas(), NamedEntity.class).size(),
				JCasUtil.select(cas1.getJCas(), NamedEntity.class).size());
		assertEquals(JCasUtil.select(cas2.getJCas(), Sentence.class).size(),
				JCasUtil.select(cas1.getJCas(), Sentence.class).size());
		assertEquals(JCasUtil.select(cas2.getJCas(), Dependency.class).size(),
				JCasUtil.select(cas1.getJCas(), Dependency.class).size());
	}
    
    @Test
    public void testZeroLengthSpansWithoutFeatureValues() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        // One at the beginning
        new NamedEntity(jcas, 0, 0).addToIndexes();

        // One at the end
        new NamedEntity(jcas, jcas.getDocumentText().length(), jcas.getDocumentText().length())
                .addToIndexes();

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }
    
    @Test
    public void testZeroLengthSpansWithFeatureValues() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        // One at the beginning
        NamedEntity ne1 = new NamedEntity(jcas, 0, 0);
        ne1.setValue("PERSON");
        ne1.addToIndexes();

        // One at the end
        NamedEntity ne2 = new NamedEntity(jcas, jcas.getDocumentText().length(), jcas.getDocumentText().length());
        ne2.setValue("ORG");
        ne2.addToIndexes();

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testZeroLengthSpansWithoutFeatures() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        CAS cas = jcas.getCas();
        
        Type simpleSpanType = cas.getTypeSystem().getType("webanno.custom.SimpleSpan");
        
        // One at the beginning
        AnnotationFS fs1 = cas.createAnnotation(simpleSpanType, 0, 0);
        cas.addFsToIndexes(fs1);

        // One at the end
        AnnotationFS fs2 = cas.createAnnotation(simpleSpanType, jcas.getDocumentText().length(),
                jcas.getDocumentText().length());
        cas.addFsToIndexes(fs2);

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList("webanno.custom.SimpleSpan"));
    }

    @Test
    public void testTokenBoundedSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        int n = 0;
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue("NE " + n);
            ne.addToIndexes();
            n++;
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedStackedSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne1 = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne1.setValue("NE");
            ne1.addToIndexes();
            
            NamedEntity ne2 = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne2.setValue("NE");
            ne2.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedSpanWithNastyFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue("de.tudarmstadt.ukp.dkpro.core.api.ner.type.NamedEntity:value");
            ne.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedSpanWithUnderscoreFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue("_");
            ne.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedSpanWithAsteriskFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue("*");
            ne.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedBioLookAlike() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        int n = 0;
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue(((n == 0) ? "B-" : "I-")+"NOTBIO!");
            ne.addToIndexes();
            n++;
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testTokenBoundedSpanWithSpecialSymbolsValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        for (Token t : select(jcas, Token.class)) {
            NamedEntity ne = new NamedEntity(jcas, t.getBegin(), t.getEnd());
            ne.setValue("#*'\"`´\t:;{}|[ ]()\\§$%?=&_\n");
            ne.addToIndexes();
        }
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }
    
    @Test
    public void testMultiTokenSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        NamedEntity ne = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne.addToIndexes();
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testMultiTokenStackedSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        NamedEntity ne1 = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne1.addToIndexes();

        NamedEntity ne2 = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne2.addToIndexes();

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testMultiTokenSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        NamedEntity ne = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne.setValue("PERSON");
        ne.addToIndexes();
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }
    
    @Test
    public void testMultiTokenStackedSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasOneSentence();
        
        NamedEntity ne1 = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne1.setValue("PERSON");
        ne1.addToIndexes();

        NamedEntity ne2 = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne2.setValue("LOCATION");
        ne2.addToIndexes();

        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testCrossSentenceSpanWithoutFeatureValue() throws Exception
    {
        JCas jcas = makeJCasTwoSentences();
        
        NamedEntity ne = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne.addToIndexes();
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    @Test
    public void testCrossSentenceSpanWithFeatureValue() throws Exception
    {
        JCas jcas = makeJCasTwoSentences();
        
        NamedEntity ne = new NamedEntity(jcas, 0, jcas.getDocumentText().length());
        ne.setValue("PERSON");
        ne.addToIndexes();
        
        writeAndAssertEquals(jcas, 
                WebannoTsv3Writer.PARAM_SPAN_LAYERS, asList(NamedEntity.class));
    }

    private void writeAndAssertEquals(JCas aJCas, Object... aParams)
        throws IOException, ResourceInitializationException, AnalysisEngineProcessException
    {
        String targetFolder = "target/test-output/" + testContext.getTestOutputFolderName();
        String referenceFolder = "src/test/resources/" + testContext.getTestOutputFolderName();
        
        List<Object> params = new ArrayList<>();
        params.addAll(asList(aParams));
        params.add(WebannoTsv3Writer.PARAM_TARGET_LOCATION);
        params.add(targetFolder);
        
        AnalysisEngineDescription writer = createEngineDescription(WebannoTsv3Writer.class,
                params.toArray(new Object[params.size()]));
        
        SimplePipeline.runPipeline(aJCas, writer);
        
        String reference = FileUtils.readFileToString(new File(referenceFolder, "reference.tsv"),
                "UTF-8");
        
        String actual = FileUtils.readFileToString(new File(targetFolder, "doc.tsv"), "UTF-8");
        
        assertEquals(reference, actual);
    }

    private JCas makeJCasOneSentence() throws UIMAException
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");
       
        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));
        
        JCas jcas = JCasFactory.createJCas(merged);
        
        DocumentMetaData.create(jcas).setDocumentId("doc");
        
        TokenBuilder<Token, Sentence> tb = new TokenBuilder<Token, Sentence>(Token.class,
                Sentence.class);
        tb.buildTokens(jcas, "This is a test .");
        
        return jcas;
    }

    private JCas makeJCasTwoSentences() throws UIMAException
    {
        TypeSystemDescription global = TypeSystemDescriptionFactory.createTypeSystemDescription();
        TypeSystemDescription local = TypeSystemDescriptionFactory
                .createTypeSystemDescriptionFromPath(
                        "src/test/resources/desc/type/webannoTestTypes.xml");
       
        TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(asList(global, local));
        
        JCas jcas = JCasFactory.createJCas(merged);
        
        DocumentMetaData.create(jcas).setDocumentId("doc");
        
        TokenBuilder<Token, Sentence> tb = new TokenBuilder<Token, Sentence>(Token.class,
                Sentence.class);
        tb.buildTokens(jcas, "He loves her .\nShe loves him not .");
        
        assertEquals(2, select(jcas, Sentence.class).size());
        
        return jcas;
    }

    @Rule
    public DkproTestContext testContext = new DkproTestContext();
}
