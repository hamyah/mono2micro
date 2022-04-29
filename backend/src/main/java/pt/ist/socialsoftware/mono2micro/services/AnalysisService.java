package pt.ist.socialsoftware.mono2micro.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ist.socialsoftware.mono2micro.domain.Cluster;
import pt.ist.socialsoftware.mono2micro.domain.Codebase;
import pt.ist.socialsoftware.mono2micro.domain.Decomposition;
import pt.ist.socialsoftware.mono2micro.domain.Dendrogram;
import pt.ist.socialsoftware.mono2micro.dto.AnalyserDto;
import pt.ist.socialsoftware.mono2micro.manager.CodebaseManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static pt.ist.socialsoftware.mono2micro.utils.Constants.CODEBASES_PATH;

@Service
public class AnalysisService {

    @Autowired
    DendrogramService dendrogramService;

    @Autowired
    ClusterService clusterService;

    private final CodebaseManager codebaseManager = CodebaseManager.getInstance();

    private ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final List<String> LINKAGE_TYPES = Arrays.asList(new String[]{"average", "single", "complete"});
    private final Integer MIN_DEPTH = 1;
    private final Integer MAX_DEPTH = 6;
    private final Integer MIN_WEIGHT = 0;
    private final Integer MAX_WEIGHT = 100;
    private final Integer WEIGHT_STEP = 10;
    private final Integer DEPTH_STEP = 1;

    public void analyzeDendrogramCutsByEntitiesStrategy(
            String codebaseName,
            AnalyserDto analyserDto
    ) {
        try {
            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Dendrogram dendrogram = new Dendrogram();
            dendrogram.setAnalysisType("entities");
            dendrogram.setProfile(analyserDto.getProfile());

            File analyserFeaturesPath = new File(CODEBASES_PATH + codebaseName + "/analyser/entities");
            if (!analyserFeaturesPath.exists()) {
                analyserFeaturesPath.mkdirs();
            }

            File analyserPath = new File(CODEBASES_PATH + codebaseName + "/analyser/entities/cuts");
            if (!analyserPath.exists()) {
                analyserPath.mkdirs();
            }

            for (String lt : LINKAGE_TYPES) {
                dendrogram.setLinkageType(lt);
                dendrogramService.createDendrogramByEntitiesStrategy(codebaseName, dendrogram, true);
                clusterService.executeClusterAnalysis(codebaseName, "/entities");
            }

            JSONObject analyserResult = getAnalyserResult(
                    codebase,
                    dendrogram.getAnalysisType(),
                    dendrogram.getFeatureVectorizationStrategy(),
                    dendrogram.getProfile(),
                    analyserDto,
                    "/analyser/entities/cuts/"
            );

            FileWriter file = new FileWriter(CODEBASES_PATH + codebaseName + "/analyser/entities/analyserResult.json");
            file.write(analyserResult.toString(4));
            file.close();

            JSONObject analisysStats = new JSONObject();
            analisysStats.put("complexity", getAnalysisStats("complexity", analyserResult));
            analisysStats.put("performance", getAnalysisStats("performance", analyserResult));
            analisysStats.put("cohesion", getAnalysisStats("cohesion", analyserResult));
            analisysStats.put("coupling", getAnalysisStats("coupling", analyserResult));

            FileWriter statsFile = new FileWriter(CODEBASES_PATH + codebaseName + "/analyser/entities/analisysStats.json");
            statsFile.write(analisysStats.toString(4));
            statsFile.close();

            clusterService.executePlotAnalysis(codebaseName, "entities", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void analyzeDendrogramCutsByClassesStrategy(
            String codebaseName,
            AnalyserDto analyserDto
    ) {
        try {
            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Dendrogram dendrogram = new Dendrogram();
            dendrogram.setAnalysisType("class");
            dendrogram.setProfile(analyserDto.getProfile());

            File analyserFeaturesPath = new File(CODEBASES_PATH + codebaseName + "/analyser/classes/");
            if (!analyserFeaturesPath.exists()) {
                analyserFeaturesPath.mkdirs();
            }

            File analyserPath = new File(CODEBASES_PATH + codebaseName + "/analyser/classes/cuts");
            if (!analyserPath.exists()) {
                analyserPath.mkdirs();
            }

            for (String lt : LINKAGE_TYPES) {
                dendrogram.setLinkageType(lt);
                dendrogramService.createDendrogramByClassesStrategy(codebaseName, dendrogram, true);
                clusterService.executeClusterAnalysis(codebaseName, "/classes");
            }

            JSONObject analyserResult = getAnalyserResult(
                    codebase,
                    dendrogram.getAnalysisType(),
                    dendrogram.getFeatureVectorizationStrategy(),
                    dendrogram.getProfile(),
                    analyserDto,
                    "/analyser/classes/cuts/"
            );

            FileWriter file = new FileWriter(CODEBASES_PATH + codebaseName + "/analyser/classes/analyserResult.json");
            file.write(analyserResult.toString(4));
            file.close();

            JSONObject analisysStats = new JSONObject();
            analisysStats.put("complexity", getAnalysisStats("complexity", analyserResult));
            analisysStats.put("performance", getAnalysisStats("performance", analyserResult));
            analisysStats.put("cohesion", getAnalysisStats("cohesion", analyserResult));
            analisysStats.put("coupling", getAnalysisStats("coupling", analyserResult));

            FileWriter statsFile = new FileWriter(CODEBASES_PATH + codebaseName + "/analyser/classes/analisysStats.json");
            statsFile.write(analisysStats.toString(4));
            statsFile.close();

            clusterService.executePlotAnalysis(codebaseName, "classes", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ConcurrentMethodCallsAnalysisThread extends Thread {

        AnalyserDto analyserDto;
        String codebaseName;
        String linkageType;
        Integer threadNumber;

        ConcurrentMethodCallsAnalysisThread(
              AnalyserDto analyserDto,
              String codebaseName,
              String linkageType,
              Integer threadNumber
        ) {
            this.analyserDto = analyserDto;
            this.codebaseName = codebaseName;
            this.linkageType = linkageType;
            this.threadNumber = threadNumber;
        }

        @Override
        public void run() {
            Dendrogram dendrogram = new Dendrogram();
            dendrogram.setAnalysisType("feature");
            dendrogram.setFeatureVectorizationStrategy("methodCalls");
            dendrogram.setProfile(analyserDto.getProfile());
            dendrogram.setLinkageType(linkageType);
            for (int d = MIN_DEPTH; d <= MAX_DEPTH; d += DEPTH_STEP) {
                dendrogram.setMaxDepth(d);
                for (int cw = MIN_WEIGHT; cw <= MAX_WEIGHT; cw += WEIGHT_STEP) {
                    dendrogram.setControllersWeight(cw);
                    for (int sw = MIN_WEIGHT; sw <= MAX_WEIGHT; sw += WEIGHT_STEP) {
                        dendrogram.setServicesWeight(sw);
                        for (int iw = MIN_WEIGHT; iw <= MAX_WEIGHT; iw += WEIGHT_STEP) {
                            dendrogram.setIntermediateMethodsWeight(iw);
                            for (int ew = MIN_WEIGHT; ew <= MAX_WEIGHT; ew += WEIGHT_STEP) {
                                if (cw + sw + iw + ew == 100) {
                                    dendrogram.setEntitiesWeight(ew);
                                    dendrogramService.createDendrogramByFeatures(codebaseName, dendrogram, true, threadNumber);
                                    clusterService.executeClusterAnalysis(codebaseName, "/features/methodCalls/" + threadNumber.toString());
                                }
                            }
                        }
                    }
                    Float perc = Float.valueOf((d + (cw / 100)) / 7);
                    System.out.println("Progress: " + perc.toString());
                }
            }
        }
    }

    public void analyzeDendrogramCutsByMethodCallsStrategy(
            String codebaseName,
            AnalyserDto analyserDto
    ) {
        try {

            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Integer threadNumber = 1;

            File analyserFeaturesPath = new File(CODEBASES_PATH + codebaseName + "/analyser/features/");
            if (!analyserFeaturesPath.exists()) {
                analyserFeaturesPath.mkdirs();
            }

            File analyserMethodCallsFeaturesPath = new File(CODEBASES_PATH + codebaseName + "/analyser/features/methodCalls/");
            if (!analyserMethodCallsFeaturesPath.exists()) {
                analyserMethodCallsFeaturesPath.mkdirs();
            }

            File analyserPath = new File(CODEBASES_PATH + codebaseName + "/analyser/features/methodCalls/cuts");
            if (!analyserPath.exists()) {
                analyserPath.mkdirs();
            }

            List<ConcurrentMethodCallsAnalysisThread> threadsPool = new ArrayList<>();
            for (String lt: LINKAGE_TYPES) {
                ConcurrentMethodCallsAnalysisThread thread = new ConcurrentMethodCallsAnalysisThread(analyserDto, codebaseName, lt, threadNumber);
                threadsPool.add(thread);
                threadNumber++;
            }

            try {
                for (ConcurrentMethodCallsAnalysisThread thread : threadsPool) {
                    thread.start();
                }
                for (ConcurrentMethodCallsAnalysisThread thread : threadsPool) {
                    thread.join();
                }
            } catch(InterruptedException ie) {
                ie.printStackTrace();
            }

            // Test with the same weights
            for (String lt: LINKAGE_TYPES) {
                Dendrogram dendrogram = new Dendrogram();
                dendrogram.setAnalysisType("feature");
                dendrogram.setFeatureVectorizationStrategy("methodCalls");
                dendrogram.setProfile(analyserDto.getProfile());
                dendrogram.setLinkageType(lt);
                for (int d = MIN_DEPTH; d <= MAX_DEPTH; d += DEPTH_STEP) {
                    dendrogram.setMaxDepth(d);
                    dendrogram.setControllersWeight(25);
                    dendrogram.setServicesWeight(25);
                    dendrogram.setIntermediateMethodsWeight(25);
                    dendrogram.setEntitiesWeight(25);
                    dendrogramService.createDendrogramByFeatures(codebaseName, dendrogram, true, null);
                    clusterService.executeClusterAnalysis(codebaseName, "/features/methodCalls");
                }
            }

            JSONObject analyserResult = getAnalyserResult(
                    codebase,
                    "feature",
                    "methodCalls",
                    analyserDto.getProfile(),
                    analyserDto,
                    "/analyser/features/methodCalls/cuts/"
            );

            FileWriter file = new FileWriter(CODEBASES_PATH + codebaseName + "/analyser/features/methodCalls/analyserResult.json");
            file.write(analyserResult.toString(4));
            file.close();

            JSONObject analisysStats = new JSONObject();
            analisysStats.put("complexity", getAnalysisStats("complexity", analyserResult));
            analisysStats.put("performance", getAnalysisStats("performance", analyserResult));
            analisysStats.put("cohesion", getAnalysisStats("cohesion", analyserResult));
            analisysStats.put("coupling", getAnalysisStats("coupling", analyserResult));

            FileWriter statsFile = new FileWriter(CODEBASES_PATH + codebaseName + "/analyser/features/methodCalls/analisysStats.json");
            statsFile.write(analisysStats.toString(4));
            statsFile.close();

            clusterService.executePlotAnalysis(codebaseName, "features", "methodCalls");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void analyzeDendrogramCutsByEntitiesTracesStrategy(
            String codebaseName,
            AnalyserDto analyserDto
    ) {
        try {
            Codebase codebase = codebaseManager.getCodebase(codebaseName);
            Dendrogram dendrogram = new Dendrogram();
            dendrogram.setAnalysisType("feature");
            dendrogram.setFeatureVectorizationStrategy("entitiesTraces");
            dendrogram.setProfile(analyserDto.getProfile());

            File analyserFeaturesPath = new File(CODEBASES_PATH + codebaseName + "/analyser/features/");
            if (!analyserFeaturesPath.exists()) {
                analyserFeaturesPath.mkdirs();
            }

            File analyserMethodCallsFeaturesPath = new File(CODEBASES_PATH + codebaseName + "/analyser/features/entitiesTraces/");
            if (!analyserMethodCallsFeaturesPath.exists()) {
                analyserMethodCallsFeaturesPath.mkdirs();
            }

            File analyserPath = new File(CODEBASES_PATH + codebaseName + "/analyser/features/entitiesTraces/cuts");
            if (!analyserPath.exists()) {
                analyserPath.mkdirs();
            }

            for (String lt : LINKAGE_TYPES) {
                dendrogram.setLinkageType(lt);
                for (int wmw = MIN_WEIGHT; wmw <= MAX_WEIGHT; wmw += WEIGHT_STEP) {
                    dendrogram.setWriteMetricWeight(wmw);
                    for (int rmw = MIN_WEIGHT; rmw <= MAX_WEIGHT; rmw += WEIGHT_STEP) {
                        if (wmw + rmw == 100) {
                            dendrogram.setReadMetricWeight(rmw);
                            dendrogramService.createDendrogramByFeatures(codebaseName, dendrogram, true, null);
                            clusterService.executeClusterAnalysis(codebaseName, "/features/entitiesTraces");
                        }
                    }
                }
            }

            JSONObject analyserResult = getAnalyserResult(
                    codebase,
                    dendrogram.getAnalysisType(),
                    dendrogram.getFeatureVectorizationStrategy(),
                    dendrogram.getProfile(),
                    analyserDto,
                    "/analyser/features/entitiesTraces/cuts/"
            );

            FileWriter file = new FileWriter(CODEBASES_PATH + codebaseName + "/analyser/features/entitiesTraces/analyserResult.json");
            file.write(analyserResult.toString(4));
            file.close();

            JSONObject analisysStats = new JSONObject();
            analisysStats.put("complexity", getAnalysisStats("complexity", analyserResult));
            analisysStats.put("performance", getAnalysisStats("performance", analyserResult));
            analisysStats.put("cohesion", getAnalysisStats("cohesion", analyserResult));
            analisysStats.put("coupling", getAnalysisStats("coupling", analyserResult));

            FileWriter statsFile = new FileWriter(CODEBASES_PATH + codebaseName + "/analyser/features/entitiesTraces/analisysStats.json");
            statsFile.write(analisysStats.toString(4));
            statsFile.close();

            clusterService.executePlotAnalysis(codebaseName, "features", "entitiesTraces");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void analyzeDendrogramCutsByMixedStrategy(
            String codebaseName,
            AnalyserDto analyserDto
    ) {
        try {

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ----------------------------------------------------------------------------------------------
    // ---                                AUXILIARY FUNCTIONS                                     ---
    // ----------------------------------------------------------------------------------------------

    private JSONObject getAnalyserResult(
            Codebase codebase,
            String analysisType,
            String featureVectorizationStrategy,
            String profile,
            AnalyserDto analyserDto,
            String cutsPath
    ) throws Exception {

        File analyserCutsPath = new File(CODEBASES_PATH + codebase.getName() + cutsPath);
        File[] files = analyserCutsPath.listFiles();
        JSONObject analyserResult = new JSONObject();

        for (File file : files) {

            String filename = FilenameUtils.getBaseName(file.getName());

            Decomposition cutDecomposition = new Decomposition();

            InputStream is = new FileInputStream(analyserCutsPath + "/" + filename + ".json");
            JSONObject clustersJSON = new JSONObject(IOUtils.toString(is, StandardCharsets.UTF_8));
            is.close();

            Integer numberOfEntitiesClusters = clustersJSON.getInt("numberOfEntitiesClusters");
            Iterator<String> clusters = clustersJSON.getJSONObject("clusters").sortedKeys();
            ArrayList<Short> clusterIds = new ArrayList<>();
            while(clusters.hasNext()) {
                clusterIds.add(Short.parseShort(clusters.next()));
            }
            Collections.sort(clusterIds);

            for (Short id : clusterIds) {
                String clusterName = String.valueOf(id);
                JSONArray entities = clustersJSON.getJSONObject("clusters").getJSONArray(id.toString());
                Cluster cluster = new Cluster(id, clusterName);

                for (int i = 0; i < entities.length(); i++) {
                    short entityID = (short) entities.getInt(i);
                    cluster.addEntity(entityID);
                    cutDecomposition.putEntity(entityID, id);
                }

                cutDecomposition.addCluster(cluster);
            }

            cutDecomposition.setNextClusterID(Integer.valueOf(clusterIds.size()).shortValue());

            cutDecomposition.setControllers(codebaseManager.getControllersWithCostlyAccesses(
                    codebase,
                    profile,
                    cutDecomposition.getEntityIDToClusterID()
            ));

            cutDecomposition.calculateMetrics(
                    codebase,
                    analyserDto.getTracesMaxLimit(),
                    analyserDto.getTraceType(),
                    true
            );

            JSONObject metrics = new JSONObject();

            if (analysisType.equals("feature") && featureVectorizationStrategy.equals("methodCalls")) {
                String[] weights = filename.split(",");
                String linkageType = weights[0];
                Float maxDepth = Float.parseFloat(weights[1]);
                Float controllersWeight = Float.parseFloat(weights[2]);
                Float servicesWeight = Float.parseFloat(weights[3]);
                Float intermediateMethodsWeight = Float.parseFloat(weights[4]);
                Float entitiesWeight = Float.parseFloat(weights[5]);
                Integer clusterSize = Integer.parseInt(weights[6]);

                metrics.put("linkageType", linkageType);
                metrics.put("maxDepth", maxDepth);
                metrics.put("controllersWeight", controllersWeight);
                metrics.put("servicesWeight", servicesWeight);
                metrics.put("intermediateMethodsWeight", intermediateMethodsWeight);
                metrics.put("entitiesWeight", entitiesWeight);
                metrics.put("numberClusters", clusterSize);
            } else if (analysisType.equals("feature") && featureVectorizationStrategy.equals("entitiesTraces")) {
                String[] weights = filename.split(",");
                String linkageType = weights[0];
                Float writeMetricWeight = Float.parseFloat(weights[1]);
                Float readMetricWeight = Float.parseFloat(weights[2]);
                Integer clusterSize = Integer.parseInt(weights[3]);

                metrics.put("linkageType", linkageType);
                metrics.put("writeMetricWeight", writeMetricWeight);
                metrics.put("readMetricWeight", readMetricWeight);
                metrics.put("numberClusters", clusterSize);
            } else {
                String[] weights = filename.split(",");
                String linkageType = weights[0];
                Integer clusterSize = Integer.parseInt(weights[1]);

                metrics.put("linkageType", linkageType);
                metrics.put("numberClusters", clusterSize);
            }

            metrics.put("numberOfEntitiesClusters", numberOfEntitiesClusters);
            metrics.put("cohesion", cutDecomposition.getCohesion());
            metrics.put("coupling", cutDecomposition.getCoupling());
            metrics.put("complexity", cutDecomposition.getComplexity());
            metrics.put("performance", cutDecomposition.getPerformance());
            metrics.put("accuracy", 0.0);
            metrics.put("precision", 0.0);
            metrics.put("recall", 0.0);
            metrics.put("specificity", 0.0);
            metrics.put("fmeasure", 0.0);
            metrics.put("mojoCommon", 0.0);
            metrics.put("mojoBiggest", 0.0);
            metrics.put("mojoNew", 0.0);
            metrics.put("mojoSingletons", 0.0);
            metrics.put("controllerSpecs", new JSONObject());

            analyserResult.put(filename, metrics);

        }

        return analyserResult;
    }

    public JSONArray getAnalysisStats(String metric, JSONObject analyserResult) throws JSONException {
        JSONArray clusters_stats = new JSONArray();
        for (int i = 0; i < 10; i++) {
            JSONObject initJson = new JSONObject();
            initJson.put("mean", 0.0);
            initJson.put("min", Double.MAX_VALUE);
            initJson.put("max", 0.0);
            initJson.put("counter", 0);
            clusters_stats.put(i, initJson);
        }
        Iterator<String> keys = analyserResult.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            if (analyserResult.get(key) instanceof JSONObject) {
                JSONObject analysis = (JSONObject) analyserResult.get(key);
                Double value = analysis.getDouble(metric);
                Integer idx = analysis.getInt("numberOfEntitiesClusters") - 1;
                JSONObject updated_stats = clusters_stats.getJSONObject(idx);
                updated_stats.put("mean", updated_stats.getDouble("mean") + value);
                if (value > updated_stats.getDouble("max")) {
                    updated_stats.put("max", value);
                }
                if (value < updated_stats.getDouble("min")) {
                    updated_stats.put("min", value);
                }
                updated_stats.put("counter", updated_stats.getInt("counter") + 1);
                clusters_stats.put(idx, updated_stats);
            }
        }
        for (int i = 0; i < 10; i++) {
            JSONObject updated_stats = clusters_stats.getJSONObject(i);
            if (updated_stats.getInt("counter") != 0) {
                updated_stats.put("mean", updated_stats.getDouble("mean") / updated_stats.getInt("counter"));
            } else {
                updated_stats.put("min", 0);
            }
        }
        return clusters_stats;
    }

}
