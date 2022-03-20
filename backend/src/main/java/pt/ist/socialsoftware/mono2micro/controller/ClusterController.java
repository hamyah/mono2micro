package pt.ist.socialsoftware.mono2micro.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.ist.socialsoftware.mono2micro.domain.*;
import pt.ist.socialsoftware.mono2micro.domain.source.AccessesSource;
import pt.ist.socialsoftware.mono2micro.domain.strategy.AccessesSciPyStrategy;
import pt.ist.socialsoftware.mono2micro.manager.CodebaseManager;
import pt.ist.socialsoftware.mono2micro.utils.Utils;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.util.*;

import static pt.ist.socialsoftware.mono2micro.domain.source.Source.SourceType.ACCESSES;

@RestController
@RequestMapping(value = "/mono2micro/codebase/{codebaseName}/strategy/{strategyName}/decomposition/{decompositionName}")
public class ClusterController {

	private static final Logger logger = LoggerFactory.getLogger(ClusterController.class);

	private final CodebaseManager codebaseManager = CodebaseManager.getInstance();

	@RequestMapping(value = "/cluster/{clusterNameID}/merge", method = RequestMethod.POST)
	public ResponseEntity<HttpStatus> mergeClusters(
		@PathVariable String codebaseName,
		@PathVariable String strategyName,
		@PathVariable String decompositionName,
		@PathVariable Short clusterNameID,
		@RequestParam Short otherClusterID,
		@RequestParam String newName
	) {
		logger.debug("mergeClusters");

		try {
			// FIXME Each dendrogram directory would have a folder for controllers and another for clusters
			// FIXME Each controller and cluster would have its own json file

			AccessesSource source = (AccessesSource) codebaseManager.getCodebaseSource(codebaseName, ACCESSES);
			AccessesSciPyStrategy strategy = (AccessesSciPyStrategy) codebaseManager.getCodebaseStrategy(codebaseName, strategyName);
			Decomposition decomposition = codebaseManager.getStrategyDecompositionWithFields(codebaseName, strategyName, decompositionName, null);

			decomposition.mergeClusters(
				clusterNameID,
				otherClusterID,
				newName
			);

			decomposition.setControllers(codebaseManager.getControllersWithCostlyAccesses(
				source.getInputFilePath(),
				source.getProfile(strategy.getProfile()),
				decomposition.getEntityIDToClusterID()
			));

			decomposition.calculateMetrics(
				source.getInputFilePath(),
				strategy.getTracesMaxLimit(),
				strategy.getTraceType(),
					false);

			codebaseManager.writeStrategyDecomposition(codebaseName, strategyName, decomposition);
			return new ResponseEntity<>(HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/cluster/{clusterID}/rename", method = RequestMethod.POST)
	public ResponseEntity<HttpStatus> renameCluster(
		@PathVariable String codebaseName,
		@PathVariable String strategyName,
		@PathVariable String decompositionName,
		@PathVariable Short clusterID,
		@RequestParam String newName
	) {
		logger.debug("renameCluster");

		try {
			// FIXME Each dendrogram directory would have a folder for controllers and another for clusters
			// FIXME Each controller and cluster would have its own json file

			Decomposition decomposition = codebaseManager.getStrategyDecompositionWithFields(codebaseName, strategyName, decompositionName, null);

			decomposition.renameCluster(
				clusterID,
				newName
			);

			codebaseManager.writeStrategyDecomposition(codebaseName, strategyName, decomposition);
			return new ResponseEntity<>(HttpStatus.OK);

		} catch (KeyAlreadyExistsException e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/cluster/{clusterID}/split", method = RequestMethod.POST)
	public ResponseEntity<HttpStatus> splitCluster(
		@PathVariable String codebaseName,
		@PathVariable String strategyName,
		@PathVariable String decompositionName,
		@PathVariable Short clusterID,
		@RequestParam String newName,
		@RequestParam String entities
	) {
		logger.debug("splitCluster");

		try {
			// FIXME Each dendrogram directory would have a folder for controllers and another for clusters
			// FIXME Each controller and cluster would have its own json file

			AccessesSource source = (AccessesSource) codebaseManager.getCodebaseSource(codebaseName, ACCESSES);
			AccessesSciPyStrategy strategy = (AccessesSciPyStrategy) codebaseManager.getCodebaseStrategy(codebaseName, strategyName);
			Decomposition decomposition = codebaseManager.getStrategyDecompositionWithFields(codebaseName, strategyName, decompositionName, null);

			decomposition.splitCluster(
				clusterID,
				newName,
				entities.split(",")
			);

			decomposition.setControllers(codebaseManager.getControllersWithCostlyAccesses(
					source.getInputFilePath(),
					source.getProfile(strategy.getProfile()),
					decomposition.getEntityIDToClusterID()
			));

			decomposition.calculateMetrics(
					source.getInputFilePath(),
					strategy.getTracesMaxLimit(),
					strategy.getTraceType(),
					false);

			codebaseManager.writeStrategyDecomposition(codebaseName, strategyName, decomposition);
			return new ResponseEntity<>(HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/cluster/{clusterID}/transferEntities", method = RequestMethod.POST)
	public ResponseEntity<HttpStatus> transferEntities(
		@PathVariable String codebaseName,
		@PathVariable String strategyName,
		@PathVariable String decompositionName,
		@PathVariable Short clusterID,
		@RequestParam Short toClusterID,
		@RequestParam String entities
	) {
		logger.debug("transferEntities");

		try {
			// FIXME Each dendrogram directory would have a folder for controllers and another for clusters
			// FIXME Each controller and cluster would have its own json file

			AccessesSource source = (AccessesSource) codebaseManager.getCodebaseSource(codebaseName, ACCESSES);
			AccessesSciPyStrategy strategy = (AccessesSciPyStrategy) codebaseManager.getCodebaseStrategy(codebaseName, strategyName);
			Decomposition decomposition = codebaseManager.getStrategyDecompositionWithFields(codebaseName, strategyName, decompositionName, null);

			decomposition.transferEntities(
				clusterID,
				toClusterID,
				entities.split(",")
			);

			decomposition.setControllers(codebaseManager.getControllersWithCostlyAccesses(
				source.getInputFilePath(),
				source.getProfile(strategy.getProfile()),
				decomposition.getEntityIDToClusterID()
			));

			decomposition.calculateMetrics(
				source.getInputFilePath(),
				strategy.getTracesMaxLimit(),
				strategy.getTraceType(),
					false);

			codebaseManager.writeStrategyDecomposition(codebaseName, strategyName, decomposition);
			return new ResponseEntity<>(HttpStatus.OK);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/controllersClusters", method = RequestMethod.GET)
	public ResponseEntity<Map<String, Set<Cluster>>> getControllersClusters(
		@PathVariable String codebaseName,
		@PathVariable String strategyName,
		@PathVariable String decompositionName
	) {
		logger.debug("getControllersClusters");

		try {
			Decomposition decomposition = codebaseManager.getStrategyDecompositionWithFields(
				codebaseName,
				strategyName,
				decompositionName,
				new HashSet<String>() {{ add("clusters"); add("controllers"); }}
			);

			Utils.GetControllersClustersAndClustersControllersResult result =
				Utils.getControllersClustersAndClustersControllers(
					decomposition.getClusters().values(),
					decomposition.getControllers().values()
				);

			return new ResponseEntity<>(
				result.controllersClusters,
				HttpStatus.OK
			);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/clustersControllers", method = RequestMethod.GET)
	public ResponseEntity<Map<Short, Set<Controller>>> getClustersControllers(
		@PathVariable String codebaseName,
		@PathVariable String strategyName,
		@PathVariable String decompositionName
	) {
		logger.debug("getClustersControllers");

		try {
			Decomposition decomposition = codebaseManager.getStrategyDecompositionWithFields(
				codebaseName,
				strategyName,
				decompositionName,
				new HashSet<String>() {{ add("clusters"); add("controllers"); }}
			);

			Utils.GetControllersClustersAndClustersControllersResult result =
				Utils.getControllersClustersAndClustersControllers(
					decomposition.getClusters().values(),
					decomposition.getControllers().values()
				);

			return new ResponseEntity<>(
				result.clustersControllers,
				HttpStatus.OK
			);

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
	}
}