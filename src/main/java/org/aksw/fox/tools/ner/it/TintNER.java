package org.aksw.fox.tools.ner.it;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import eu.fbk.dh.tint.runner.TintPipeline;
import org.aksw.fox.data.Entity;
import org.aksw.fox.data.EntityClassMap;
import org.aksw.fox.tools.ner.AbstractNER;
import org.aksw.fox.utils.FoxConst;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author Varma on 05-01-2018
 * @project fox
 * <p>
 * Tint (The Italian NLP Tool) is a Java-based pipeline for Natural Language Processing (NLP) in Italian.
 * The tool is based on Stanford CoreNLP.
 */

public class TintNER extends AbstractNER {


    /**
     * <code>
     * <p>
     * https://github.com/dhfbk/tint/blob/master/tint-runner/src/main/resources/default-config.properties
     * <p>
     * </code>
     */
    private static Properties props = new Properties();

    static {
        props.setProperty("annotators", "ita_toksent, pos, ita_morpho, ita_lemma, ner, depparse, fake_dep");
        props.setProperty("pos.model", "models/italian-big.tagger");
        props.setProperty("ner.model", "models/ner-ita-nogpe-noiob_gaz_wikipedia_sloppy.ser.gz");
        props.setProperty("ner.useSUTime", "0");
        props.setProperty("depparse.model", "models/parser-model-1.txt.gz");
        props.setProperty("tokenize.language", "it");
    }


    protected TintPipeline pipeline = null;

    private TintNER() {
        pipeline = new TintPipeline(props);

        entityClasses.put("PERSON", EntityClassMap.P);
        entityClasses.put("LOCATION", EntityClassMap.L);
        entityClasses.put("ORGANIZATION", EntityClassMap.O);

    }


    // Entity types check and mapping .. done
    private static final Map<String, String> ENTITY_MAP = new HashMap<>();

    static {
        ENTITY_MAP.put("PER", EntityClassMap.P);
        ENTITY_MAP.put("LOC", EntityClassMap.L);
        ENTITY_MAP.put("ORG", EntityClassMap.O);
    }

    // The following code is duplicate from StanfordCommon, since TINT is based on CoreNLP expecting the retrieve method
    // to work the same.
    @Override
    public List<Entity> retrieve(String input) {

        LOG.info("retrieve ...");

        // Get the original Annotation (Stanford CoreNLP)
        edu.stanford.nlp.pipeline.Annotation ann = pipeline.runRaw(input);

        final List<Entity> list = new java.util.ArrayList<>();

        for (final CoreMap sentence : ann.get(CoreAnnotations.SentencesAnnotation.class)) {
            String tokensentence = "";
            for (final CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                tokensentence += token.word() + " ";
                final String type = mapTypeToSupportedType(token.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                final String currentToken = token.originalText();
                // check for multiword entities
                boolean contains = false;
                boolean equalTypes = false;
                Entity lastEntity = null;
                if (!list.isEmpty()) {
                    lastEntity = list.get(list.size() - 1);
                    contains = tokensentence.contains(lastEntity.getText() + " " + currentToken + " ");
                    equalTypes = type.equals(lastEntity.getType());
                }
                if (contains && equalTypes) {
                    lastEntity.addText(currentToken);
                } else {
                    if (type != EntityClassMap.getNullCategory()) {
                        final float p = Entity.DEFAULT_RELEVANCE;
                        list.add(getEntity(currentToken, type, p, getToolName()));
                    }
                }
            }
        }
        // TRACE
        if (LOG.isTraceEnabled()) {
            LOG.trace(list);
        } // TRACE
        LOG.info("retrieve done.");
        return list;
    }

    public static void main(final String[] a) {
        LOG.info(new TintNER().retrieve(FoxConst.NER_IT_EXAMPLE_1));
    }
}