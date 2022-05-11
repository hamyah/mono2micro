package pt.ist.socialsoftware.mono2micro.controller;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import pt.ist.socialsoftware.mono2micro.domain.Cluster;
import pt.ist.socialsoftware.mono2micro.domain.Codebase;
import pt.ist.socialsoftware.mono2micro.domain.Functionality;
import pt.ist.socialsoftware.mono2micro.domain.clusteringAlgorithm.ClusteringAlgorithm;
import pt.ist.socialsoftware.mono2micro.domain.clusteringAlgorithm.ClusteringAlgorithmFactory;
import pt.ist.socialsoftware.mono2micro.domain.decomposition.AccessesSciPyDecomposition;
import pt.ist.socialsoftware.mono2micro.domain.similarityGenerator.SimilarityGenerator;
import pt.ist.socialsoftware.mono2micro.domain.similarityGenerator.SimilarityGeneratorFactory;
import pt.ist.socialsoftware.mono2micro.domain.source.AccessesSource;
import pt.ist.socialsoftware.mono2micro.domain.strategy.AccessesSciPyStrategy;
import pt.ist.socialsoftware.mono2micro.domain.strategy.RecommendAccessesSciPyStrategy;
import pt.ist.socialsoftware.mono2micro.domain.strategy.Strategy;
import pt.ist.socialsoftware.mono2micro.dto.*;
import pt.ist.socialsoftware.mono2micro.manager.CodebaseManager;
import pt.ist.socialsoftware.mono2micro.utils.Utils;
import pt.ist.socialsoftware.mono2micro.utils.mojoCalculator.src.main.java.MoJo;

import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static pt.ist.socialsoftware.mono2micro.domain.source.Source.SourceType.ACCESSES;
import static pt.ist.socialsoftware.mono2micro.utils.Constants.*;

@RestController
@RequestMapping(value = "/mono2micro")
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);
    private final CodebaseManager codebaseManager = CodebaseManager.getInstance();

	@RequestMapping(value = "/codebase/{codebaseName}/recommendation", method = RequestMethod.PUT)
	public ResponseEntity<Strategy> recommendation(
			@PathVariable String codebaseName,
			@RequestBody Strategy strategy
	) {
		logger.debug("recommendation");

		try {
			switch (strategy.getType()) {
				case Strategy.StrategyType.RECOMMENDATION_ACCESSES_SCIPY:
					return recommendAccessesSciPy(codebaseName, (RecommendAccessesSciPyStrategy) strategy);
				default:
					throw new RuntimeException("Unknown recommendation type:" + strategy.getType());
			}
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	public ResponseEntity<Strategy> recommendAccessesSciPy(
				String codebaseName,
				RecommendAccessesSciPyStrategy strategyRecommendation
	) throws IOException {
		List<Strategy> strategies = codebaseManager.getCodebaseStrategies(codebaseName, RECOMMEND_FOLDER, Collections.singletonList(strategyRecommendation.getType()));

		RecommendAccessesSciPyStrategy existingStrategy = (RecommendAccessesSciPyStrategy) strategies.stream()
				.filter(strategy -> strategy.equals(strategyRecommendation)).findFirst().orElse(null);

		RecommendAccessesSciPyStrategy strategy;
		// Create from scratch
		if (existingStrategy == null) {
			strategyRecommendation.setCompleted(false);
			strategyRecommendation.addCombinationsInProduction();
			strategy = (RecommendAccessesSciPyStrategy) codebaseManager.createCodebaseStrategy(codebaseName, RECOMMEND_FOLDER, strategyRecommendation);
		}
		// Already satisfies the requirements
		else {
			existingStrategy.setTraceTypes(strategyRecommendation.getTraceTypes());
			existingStrategy.setLinkageTypes(strategyRecommendation.getLinkageTypes());

			if (existingStrategy.containsRequestedCombinations() || !existingStrategy.isCompleted())
				return new ResponseEntity<>(existingStrategy, HttpStatus.CREATED);
			// Adds to the already existing strategy
			else {
				existingStrategy.addCombinationsInProduction();
				existingStrategy.setCompleted(false);
				codebaseManager.writeCodebaseStrategy(codebaseName, RECOMMEND_FOLDER, existingStrategy);
				strategy = existingStrategy;
			}
		}

		// Executes the request in a fork to avoid blocking the user
		ForkJoinPool.commonPool().submit(() -> {
			try {
				SimilarityGenerator similarityGenerator = SimilarityGeneratorFactory.getFactory().getSimilarityGenerator(strategy.getType());
				ClusteringAlgorithm clusteringAlgorithm = ClusteringAlgorithmFactory.getFactory().getClusteringAlgorithm(strategy.getType());

				similarityGenerator.createSimilarityMatrix(strategy);
				clusteringAlgorithm.createDecomposition(strategy, null);

				strategy.addProducedCombinations();
				codebaseManager.writeCodebaseStrategy(codebaseName, RECOMMEND_FOLDER, strategy);
				logger.debug("recommendation ended");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		return new ResponseEntity<>(strategy, HttpStatus.OK);
	}

	@RequestMapping(value = "/codebase/{codebaseName}/recommendationStrategy/{strategyName}/getRecommendationResult", method = RequestMethod.GET)
	public ResponseEntity<String> getRecommendationResult(
			@PathVariable String codebaseName,
			@PathVariable String strategyName
	) {
		logger.debug("getRecommendationResult");

		try {
			return new ResponseEntity<>(codebaseManager.getRecommendationResult(codebaseName, strategyName), HttpStatus.OK);

		} catch (FileNotFoundException e) { // Since it is an asynchronous call, the file might not be created yet
			return new ResponseEntity<>(null, HttpStatus.OK);
		} catch (JSONException | IOException e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/codebase/{codebaseName}/recommendationStrategy/{strategyName}/createDecompositions")
	public ResponseEntity<HttpStatus> createDecompositions(
			@PathVariable String codebaseName,
			@PathVariable String strategyName,
			@RequestParam List<String> decompositionNames
	) {
		try {
			logger.debug("createDecompositions");

			RecommendAccessesSciPyStrategy recommendationStrategy = (RecommendAccessesSciPyStrategy) codebaseManager.getCodebaseStrategy(codebaseName, RECOMMEND_FOLDER, strategyName);
			AccessesSource source = (AccessesSource) codebaseManager.getCodebaseSource(codebaseName, ACCESSES);

			for (String name: decompositionNames) {
				String[] properties = name.split(",");

				if (properties.length != 7)
					continue;

				TraceType traceType = TraceType.valueOf(properties[4]);
				String linkageType = properties[5];

				System.out.println("Creating decomposition with name: " + name);

				AccessesSciPyStrategy strategyInformation = new AccessesSciPyStrategy(recommendationStrategy, traceType, linkageType, name);

				// Get or create the decomposition's strategy
				List<Strategy> strategies = codebaseManager.getCodebaseStrategies(codebaseName, STRATEGIES_FOLDER, Collections.singletonList(strategyInformation.getType()));
				AccessesSciPyStrategy strategy = (AccessesSciPyStrategy) strategies.stream().filter(possibleStrategy -> possibleStrategy.equals(strategyInformation)).findFirst().orElse(null);
				if (strategy == null) {
					strategy = (AccessesSciPyStrategy) codebaseManager.createCodebaseStrategy(codebaseName, STRATEGIES_FOLDER, strategyInformation);

					codebaseManager.transferSimilarityMatrixFromRecommendation(
							codebaseName,
							recommendationStrategy.getName(),
							properties[0] + "," + properties[1] + "," + properties[2] + "," + properties[3] + "," + properties[4] + "," + properties[5] + ".json",
							"similarityMatrix.json",
							strategy);

					// generate dendrogram image
					ClusteringAlgorithm clusteringAlgorithm = ClusteringAlgorithmFactory.getFactory().getClusteringAlgorithm(strategy.getType());
					clusteringAlgorithm.createDendrogram(strategy);
				}

				//Create the decomposition by copying the existing decomposition
				AccessesSciPyDecomposition decomposition = (AccessesSciPyDecomposition) codebaseManager
						.transferDecompositionFromRecommendation(
								codebaseName,
								recommendationStrategy.getName(),
								name,
								getDecompositionName(strategy, "N" + properties[6]),
								strategy);

				// This is done since previous coupling dependencies mess up the results during 'setupFunctionalities'
				for (Cluster cluster : decomposition.getClusters().values())
					cluster.clearCouplingDependencies();

				// Fill information regarding functionalities and their redesigns
				decomposition.setupFunctionalities(
						source.getInputFilePath(),
						source.getProfile(strategy.getProfile()),
						strategy.getTracesMaxLimit(),
						strategy.getTraceType(),
						true);

				// save strategy and decomposition
				codebaseManager.writeStrategyDecomposition(codebaseName, STRATEGIES_FOLDER, decomposition.getStrategyName(), decomposition);
				codebaseManager.writeCodebaseStrategy(codebaseName, STRATEGIES_FOLDER, strategy);
			}
			logger.debug("decomposition creation ended");
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	private String getDecompositionName(AccessesSciPyStrategy strategy, String name) {
		if (strategy.getDecompositionsNames().contains(name)) {
			int i = 2;
			while (strategy.getDecompositionsNames().contains(name + "(" + i + ")"))
				i++;
			return name + "(" + i + ")";

		} else return name;
	}

	//FIXME analyzer no longer in use
	//FIXME only here in case something is needed
	@RequestMapping(value = "/codebase/{codebaseName}/analyser", method = RequestMethod.POST)
	public ResponseEntity<HttpStatus> analyser(
		@PathVariable String codebaseName,
		@RequestBody AnalyserDto analyser
	) {
		logger.debug("analyser");

		try {
			File analyserPath = new File(CODEBASES_PATH + codebaseName + "/analyser/cuts/");
			if (!analyserPath.exists()) {
				analyserPath.mkdirs();
			}

			Codebase codebase = CodebaseManager.getInstance().getCodebaseWithFields(
				codebaseName,
				new HashSet<String>() {{
					add("name");
					add("profiles");
					add("datafilePath");
				}}
			);

			int numberOfEntitiesPresentInCollection = getOrCreateSimilarityMatrix(
				codebase,
				analyser
			);

			System.out.println("Codebase: " + codebaseName + " has " + numberOfEntitiesPresentInCollection + " entities");

			executeCreateCuts(
				codebaseName,
				numberOfEntitiesPresentInCollection
			);

			// THIS FIRST PHASE EXISTS TO NOT PROCESS CUTS PREVIOUSLY PROCESSED
			// BASICALLY IT'S A COPY OF THE PREVIOUS FILE INTO THE NEW FILE

			File analyserCutsPath = new File(CODEBASES_PATH + codebaseName + "/analyser/cuts/");
			File[] files = analyserCutsPath.listFiles();
			int totalNumberOfFiles = files.length;

			ObjectMapper mapper = new ObjectMapper();
			JsonFactory jsonfactory = mapper.getFactory();

			boolean analyserResultFileAlreadyExists = codebaseManager.analyserResultFileAlreadyExists(codebaseName);

			String analyserResultFilename =  analyserResultFileAlreadyExists ?
				"newAnalyserResult.json" :
				"analyserResult.json";

			JsonGenerator jGenerator = jsonfactory.createGenerator(
				new FileOutputStream(CODEBASES_PATH + codebaseName + "/analyser/" + analyserResultFilename),
				JsonEncoding.UTF8
			);

		 	jGenerator.useDefaultPrettyPrinter();
			jGenerator.writeStartObject();

			Set<String> cutInfoNames = new HashSet<>();

			if (analyserResultFileAlreadyExists) {

				File existentAnalyserResultFile = new File(CODEBASES_PATH + codebaseName + "/analyser/analyserResult.json");

				cutInfoNames = Utils.getJsonFileKeys(existentAnalyserResultFile);

				if (cutInfoNames.size() == totalNumberOfFiles) {
					System.out.println("Analyser Complete");
					return new ResponseEntity<>(HttpStatus.OK);
				}

				JsonParser jsonParser = jsonfactory.createParser(existentAnalyserResultFile);
				jsonParser.nextValue();

				if (jsonParser.getCurrentToken() != JsonToken.START_OBJECT) {
					System.err.println("Json must start with a left curly brace");
					System.exit(-1);
				}

				jsonParser.nextValue();

				while (jsonParser.getCurrentToken() != JsonToken.END_OBJECT) {
					if (jsonParser.getCurrentToken() == JsonToken.START_OBJECT) {
						Utils.print("Cut name: " + jsonParser.getCurrentName(), Utils.lineno());
						cutInfoNames.add(jsonParser.currentName());

						CutInfoDto cutInfo = jsonParser.readValueAs(CutInfoDto.class);

						jGenerator.writeObjectField(jsonParser.getCurrentName(), cutInfo);

						jsonParser.nextValue();
					}
				}

				jGenerator.flush();

				existentAnalyserResultFile.delete();
			}

			int maxRequests = analyser.getRequestLimit();
			short newRequestsCount = 0;
			short count = 0;

			// AFTER COPYING PREVIOUS RESULTS, NEXT CUTS WILL BE PROCESSED AND THEIR RESULTS
			// WILL BE APPENDED TO THE NEW FILE

			for (File file : files) {

				String filename = FilenameUtils.getBaseName(file.getName());

				count++;

                if (cutInfoNames.contains(filename)) {
                    System.out.println(filename + " already analysed. " + count + "/" + totalNumberOfFiles);
                    continue;
                }

				AccessesSciPyDecomposition decomposition = buildDecompositionAndCalculateMetrics(
					analyser,
					codebase,
					filename
				);

                CutInfoDto cutInfo = assembleCutInformation(
                	analyser,
					decomposition,
					filename
				);

				jGenerator.writeObjectField(
					filename,
					cutInfo
				);

				jGenerator.flush();

				newRequestsCount++;

				System.out.println("NEW: " + filename + " : " + count + "/" + totalNumberOfFiles);
				if (newRequestsCount == maxRequests)
					break;

			}

			jGenerator.writeEndObject();
			jGenerator.close();

			if (analyserResultFileAlreadyExists) {
				File fileToBeRenamed = new File(CODEBASES_PATH + codebaseName + "/analyser/" + analyserResultFilename);
				File fileRenamed = new File(CODEBASES_PATH + codebaseName + "/analyser/analyserResult.json");

				fileToBeRenamed.renameTo(fileRenamed);
			}

			System.out.println("Analyser Complete");
		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}

	public int getOrCreateSimilarityMatrix(
		Codebase codebase,
		AnalyserDto analyser
	)
		throws Exception
	{
		//if (!codebaseManager.analyserSimilarityMatrixFileAlreadyExists(codebase.getName())) {

		//	SimilarityGenerator similarityGenerator;
		//	switch (analyser.getSimilarityGeneratorType()) {
		//		case ACCESSES_LOG:
		//		default:
		//			similarityGenerator = new DefaultSimilarityGenerator(codebase, analyser);
		//			break;
		//	}
		//	similarityGenerator.buildMatrix();

		//	similarityGenerator.writeSimilarityMatrix();

		//} else System.out.println("Similarity matrix already exists...");

		//JSONObject similarityMatrix = CodebaseManager.getInstance().getSimilarityMatrix(codebase.getName(), "analyser");
		//return similarityMatrix.getJSONArray("entities").length();
		return 0;
	}

	private void executeCreateCuts(
		String codebaseName,
		int totalNumberOfEntities
	) throws IOException {
		System.out.println("Executing analyser to create cuts...");

		WebClient.create(SCRIPTS_ADDRESS)
				.get()
				.uri("/scipy/{codebaseName}/{totalNumberOfEntities}/analyser",
						codebaseName, String.valueOf(totalNumberOfEntities))
				.exchange()
				.doOnSuccess(clientResponse -> {
					if (clientResponse.statusCode() != HttpStatus.OK)
						throw new RuntimeException("Error Code:" + clientResponse.statusCode());
				}).block();

		System.out.println("script execution has ended");
	}

	private AccessesSciPyDecomposition buildDecompositionAndCalculateMetrics(
		AnalyserDto analyser,
		Codebase codebase, // requirements: name, profiles, datafilePath
		String filename
	)
		throws Exception
	{
		AccessesSciPyDecomposition decomposition = new AccessesSciPyDecomposition();
		decomposition.setCodebaseName(codebase.getName());

		HashMap<String, HashMap<String, Set<Short>>> analyserCut = codebaseManager.getAnalyserCut(
			codebase.getName(),
			filename
		);

		Set<String> clusterIDs = analyserCut.get("clusters").keySet();

		for (String clusterName : clusterIDs) {
			Short clusterId = Short.parseShort(clusterName);
			Set<Short> entities = analyserCut.get("clusters").get(clusterName);
			Cluster cluster = new Cluster(clusterId, clusterName, entities);

			for (short entityID : entities)
				decomposition.putEntity(entityID, clusterId);

			decomposition.addCluster(cluster);
		}

		AccessesSource source = (AccessesSource) codebaseManager.getCodebaseSource(codebase.getName(), ACCESSES);

		decomposition.setupFunctionalities(
				source.getInputFilePath(),
				source.getProfile(analyser.getProfile()),
				analyser.getTracesMaxLimit(),
				analyser.getTraceType(),
				true);

		decomposition.calculateMetrics(
			//source.getInputFilePath(),
			//analyser.getTracesMaxLimit(),
			//analyser.getTraceType(),
			//true
		);

		return decomposition;
	}

	private CutInfoDto assembleCutInformation(
		AnalyserDto analyser,
		AccessesSciPyDecomposition decomposition,
		String filename
	)
		throws IOException
	{
		AnalysisDto analysisDto = new AnalysisDto();
		analysisDto.setDecomposition1(analyser.getExpert());
		analysisDto.setDecomposition2(decomposition);

		analysisDto = getAnalysis(analysisDto).getBody();

		AnalyserResultDto analyserResult = new AnalyserResultDto();
		analyserResult.setAccuracy(analysisDto.getAccuracy());
		analyserResult.setPrecision(analysisDto.getPrecision());
		analyserResult.setRecall(analysisDto.getRecall());
		analyserResult.setSpecificity(analysisDto.getSpecificity());
		analyserResult.setFmeasure(analysisDto.getFmeasure());
		analyserResult.setMojoBiggest(analysisDto.getMojoBiggest());
		analyserResult.setMojoCommon(analysisDto.getMojoCommon());
		analyserResult.setMojoSingletons(analysisDto.getMojoSingletons());
		analyserResult.setMojoNew(analysisDto.getMojoNew());

		//analyserResult.setComplexity(decomposition.getComplexity());
		//analyserResult.setCohesion(decomposition.getCohesion());
		//analyserResult.setCoupling(decomposition.getCoupling());
		//analyserResult.setPerformance(decomposition.getPerformance());

		analyserResult.setMaxClusterSize(decomposition.maxClusterSize());

		String[] similarityWeights = filename.split(",");
		analyserResult.setAccessWeight(Float.parseFloat(similarityWeights[0]));
		analyserResult.setWriteWeight(Float.parseFloat(similarityWeights[1]));
		analyserResult.setReadWeight(Float.parseFloat(similarityWeights[2]));
		analyserResult.setSequenceWeight(Float.parseFloat(similarityWeights[3]));
		analyserResult.setNumberClusters(Float.parseFloat(similarityWeights[4]));

		CutInfoDto cutInfo = new CutInfoDto();
		cutInfo.setAnalyserResultDto(analyserResult);

		HashMap<String, HashMap<String, Float>> functionalitySpecs = new HashMap<>();
		for (Functionality functionality : decomposition.getFunctionalities().values()) {
			functionalitySpecs.put(
				functionality.getName(),
				new HashMap<String, Float>() {{
					//put("complexity", functionality.getComplexity());
					//put("performance", (float) functionality.getPerformance());
				}}
			);
		}

		cutInfo.setFunctionalitySpecs(functionalitySpecs);

		return cutInfo;
	}

	@RequestMapping(value = "/analysis", method = RequestMethod.POST)
	public ResponseEntity<AnalysisDto> getAnalysis(@RequestBody AnalysisDto analysis) throws IOException {
		logger.debug("getAnalysis");

		if (analysis.getDecomposition1().getCodebaseName() == null) { // no expert cut from frontend
			return new ResponseEntity<>(analysis, HttpStatus.OK);
		}

		Map<String, Set<Short>> decomposition1 = new HashMap<>();
		for (Cluster c : ((AccessesSciPyDecomposition) analysis.getDecomposition1()).getClusters().values()) {
			decomposition1.put(c.getName(), c.getEntities());
		}

		Map<String, Set<Short>> decomposition2_CommonEntitiesOnly = new HashMap<>();
		for (Cluster c : ((AccessesSciPyDecomposition) analysis.getDecomposition2()).getClusters().values()) {
			decomposition2_CommonEntitiesOnly.put(c.getName(), c.getEntities());
		}

		List<Short> entities = new ArrayList<>();
		List<Short> notSharedEntities = new ArrayList<>();

		for (Set<Short> l1 : decomposition1.values()) {
			for (short e1ID : l1) {
				boolean inBoth = false;

				for (Set<Short> l2 : decomposition2_CommonEntitiesOnly.values()) {
					if (l2.contains(e1ID)) {
						inBoth = true;
						break;
					}
				}

				if (inBoth)
					entities.add(e1ID);
				else {
					notSharedEntities.add(e1ID);
				}
			}				
		}

		// ------------------------------------------------------------------------------------------
		Map<String, Set<Short>> decomposition2_UnassignedInBigger = decompositionCopyOf(decomposition2_CommonEntitiesOnly);
		Map.Entry<String, Set<Short>> biggerClusterEntry = null;

		for (Map.Entry<String, Set<Short>> clusterEntry : decomposition2_UnassignedInBigger.entrySet()) {
			if (biggerClusterEntry == null)
				biggerClusterEntry = clusterEntry;

			else if (clusterEntry.getValue().size() > biggerClusterEntry.getValue().size())
				biggerClusterEntry = clusterEntry;
		}

		biggerClusterEntry.getValue().addAll(notSharedEntities);

		// ------------------------------------------------------------------------------------------
		Map<String, Set<Short>> decomposition2_UnassignedInNew = decompositionCopyOf(decomposition2_CommonEntitiesOnly);
		Set<Short> newClusterForUnassignedEntities = new HashSet<>(notSharedEntities);
		decomposition2_UnassignedInNew.put("newClusterForUnnasignedEntities", newClusterForUnassignedEntities);

		// ------------------------------------------------------------------------------------------
		Map<String, Set<Short>> decomposition2_UnassignedInSingletons = decompositionCopyOf(decomposition2_CommonEntitiesOnly);
		for (int i = 0; i < notSharedEntities.size(); i++) {
			Set<Short> clusterSingletonEntity = new HashSet<>();
			clusterSingletonEntity.add(notSharedEntities.get(i));
			decomposition2_UnassignedInSingletons.put("singletonCluster" + i, clusterSingletonEntity);
		}

		int truePositive = 0;
		int falsePositive = 0;
		int trueNegative = 0;
		int falseNegative = 0;

		for (int i = 0; i < entities.size(); i++) {
			for (int j = i+1; j < entities.size(); j++) {
				short e1ID = entities.get(i);
				short e2ID = entities.get(j);

				String e1ClusterG1 = "";
				String e2ClusterG1 = "";
				String e1ClusterG2 = "";
				String e2ClusterG2 = "";

				for (String cluster : decomposition1.keySet()) {
					if (decomposition1.get(cluster).contains(e1ID)) {
						e1ClusterG1 = cluster;
					}
					if (decomposition1.get(cluster).contains(e2ID)) {
						e2ClusterG1 = cluster;
					}
				}

				for (String cluster : decomposition2_CommonEntitiesOnly.keySet()) {
					if (decomposition2_CommonEntitiesOnly.get(cluster).contains(e1ID)) {
						e1ClusterG2 = cluster;
					}
					if (decomposition2_CommonEntitiesOnly.get(cluster).contains(e2ID)) {
						e2ClusterG2 = cluster;
					}
				}

				boolean sameClusterInGraph1 = false;
				if (e1ClusterG1.equals(e2ClusterG1))
					sameClusterInGraph1 = true;
				
				boolean sameClusterInGraph2 = false;
				if (e1ClusterG2.equals(e2ClusterG2))
					sameClusterInGraph2 = true;

				if (sameClusterInGraph1 && sameClusterInGraph2)
					truePositive++;
				if (sameClusterInGraph1 && !sameClusterInGraph2)
					falseNegative++;
				if (!sameClusterInGraph1 && sameClusterInGraph2)
					falsePositive++;
				if (!sameClusterInGraph1 && !sameClusterInGraph2)
					trueNegative++;

				if (sameClusterInGraph1 != sameClusterInGraph2) {
					String[] falsePair = new String[6];
					falsePair[0] = String.valueOf(e1ID);
					falsePair[1] = e1ClusterG1;
					falsePair[2] = e1ClusterG2;
					falsePair[3] = String.valueOf(e2ID);
					falsePair[4] = e2ClusterG1;
					falsePair[5] = e2ClusterG2;

					analysis.addFalsePair(falsePair);
				}
			}
		}

		analysis.setTruePositive(truePositive);
		analysis.setTrueNegative(trueNegative);
		analysis.setFalsePositive(falsePositive);
		analysis.setFalseNegative(falseNegative);

		float accuracy;
		float precision;
		float recall;
		float specificity;
		float fmeasure;

		if (truePositive == 0 && trueNegative == 0 && falsePositive == 0 && falseNegative == 0) { // no ExpertCut submitted
			accuracy = 0;
			precision = 0;
			recall = 0;
			specificity = 0;
			fmeasure = 0;
		}
		else {
			accuracy = (float)(truePositive + trueNegative) / (truePositive + trueNegative + falsePositive + falseNegative);
			accuracy = BigDecimal.valueOf(accuracy).setScale(2, RoundingMode.HALF_UP).floatValue();

			precision = (float)truePositive / (truePositive + falsePositive);
			precision = Float.isNaN(precision) ? -1 : BigDecimal.valueOf(precision).setScale(2, RoundingMode.HALF_UP).floatValue();

			recall = (float)truePositive / (truePositive + falseNegative);
			recall = BigDecimal.valueOf(recall).setScale(2, RoundingMode.HALF_UP).floatValue();

			specificity = (float)trueNegative / (trueNegative + falsePositive);
			specificity = Float.isNaN(specificity) ? -1 : BigDecimal.valueOf(specificity).setScale(2, RoundingMode.HALF_UP).floatValue();

			fmeasure = 2 * precision * recall / (precision + recall);
			fmeasure = Float.isNaN(precision) ? -1 : BigDecimal.valueOf(fmeasure).setScale(2, RoundingMode.HALF_UP).floatValue();
		}

		analysis.setAccuracy(accuracy);
		analysis.setPrecision(precision);
		analysis.setRecall(recall);
		analysis.setSpecificity(specificity);
        analysis.setFmeasure(fmeasure);

        /*
        *******************************************
        ************ CALCULATE MOJO ***************
        *******************************************
        */
		double mojoValueCommonOnly = getMojoValue(
				decomposition2_CommonEntitiesOnly,
				decomposition1,
				decomposition2_CommonEntitiesOnly.values().stream().flatMap(Collection::stream).collect(Collectors.toSet())
		);

		double mojoValueUnassignedInBiggest = getMojoValue(
				decomposition2_UnassignedInBigger,
				decomposition1,
				decomposition2_UnassignedInBigger.values().stream().flatMap(Collection::stream).collect(Collectors.toSet())
		);

		double mojoValueUnassignedInNew = getMojoValue(
				decomposition2_UnassignedInNew,
				decomposition1,
				decomposition2_UnassignedInNew.values().stream().flatMap(Collection::stream).collect(Collectors.toSet())
		);

		double mojoValueUnassignedInSingletons = getMojoValue(
				decomposition2_UnassignedInSingletons,
				decomposition1,
				decomposition2_UnassignedInSingletons.values().stream().flatMap(Collection::stream).collect(Collectors.toSet())
		);

		analysis.setMojoCommon(mojoValueCommonOnly);
		analysis.setMojoBiggest(mojoValueUnassignedInBiggest);
		analysis.setMojoNew(mojoValueUnassignedInNew);
		analysis.setMojoSingletons(mojoValueUnassignedInSingletons);

		return new ResponseEntity<>(analysis, HttpStatus.OK);
	}

	private Map<String, Set<Short>> decompositionCopyOf(Map<String, Set<Short>> decomposition) {
		HashMap<String, Set<Short>> copy = new HashMap<>();

		for (Map.Entry<String, Set<Short>> entry : decomposition.entrySet())
			copy.put(
				entry.getKey(),
				new HashSet<>(entry.getValue())
			);

		return copy;
	}

	private double getMojoValue(
			Map<String, Set<Short>> decomposition1,
			Map<String, Set<Short>> decomposition2,
			Set<Short> entities
	)
		throws IOException
	{
		StringBuilder sbSource = new StringBuilder();
		for (Map.Entry<String, Set<Short>> clusterEntry : decomposition1.entrySet()) {
			String clusterName = clusterEntry.getKey();
			Set<Short> clusterEntities = clusterEntry.getValue();

			for (short entityID : clusterEntities) {
				if (entities.contains(entityID)) { // entity present in both decompositions
					sbSource.append("contain ")
							.append(clusterName)
							.append(" ")
							.append(entityID)
							.append("\n");
				}
			}
		}

		StringBuilder sbTarget = new StringBuilder();
		for (Map.Entry<String, Set<Short>> clusterEntry : decomposition2.entrySet()) {
			String clusterName = clusterEntry.getKey();
			Set<Short> clusterEntities = clusterEntry.getValue();

			for (short entityID : clusterEntities) {
				if (entities.contains(entityID)) { // entity present in both decompositions
					sbTarget.append("contain ")
							.append(clusterName)
							.append(" ")
							.append(entityID)
							.append("\n");
				}
			}
		}

		String distrSrcPath = MOJO_RESOURCES_PATH + "distrSrc.rsf";
		String distrTargetPath = MOJO_RESOURCES_PATH + "distrTarget.rsf";

		FileWriter srcFileWriter = new FileWriter(new File(distrSrcPath));
		srcFileWriter.write(sbSource.toString());
		srcFileWriter.close();

		FileWriter targetFileWriter = new FileWriter(new File(distrTargetPath));
		targetFileWriter.write(sbTarget.toString());
		targetFileWriter.close();

		return new MoJo().executeMojo(new String[]{
				distrSrcPath,
				distrTargetPath,
				"-fm"
		});
	}
}