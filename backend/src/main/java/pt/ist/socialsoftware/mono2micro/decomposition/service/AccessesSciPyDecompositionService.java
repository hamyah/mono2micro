package pt.ist.socialsoftware.mono2micro.decomposition.service;

import org.apache.commons.io.IOUtils;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pt.ist.socialsoftware.mono2micro.cluster.AccessesSciPyCluster;
import pt.ist.socialsoftware.mono2micro.clusteringAlgorithm.SciPyClusteringAlgorithmService;
import pt.ist.socialsoftware.mono2micro.decomposition.domain.AccessesSciPyDecomposition;
import pt.ist.socialsoftware.mono2micro.cluster.Cluster;
import pt.ist.socialsoftware.mono2micro.decomposition.repository.AccessesSciPyDecompositionRepository;
import pt.ist.socialsoftware.mono2micro.element.Element;
import pt.ist.socialsoftware.mono2micro.similarity.domain.AccessesSciPySimilarity;
import pt.ist.socialsoftware.mono2micro.similarity.domain.Similarity;
import pt.ist.socialsoftware.mono2micro.similarity.repository.SimilarityRepository;
import pt.ist.socialsoftware.mono2micro.functionality.FunctionalityRepository;
import pt.ist.socialsoftware.mono2micro.functionality.FunctionalityService;
import pt.ist.socialsoftware.mono2micro.functionality.domain.Functionality;
import pt.ist.socialsoftware.mono2micro.functionality.domain.FunctionalityRedesign;
import pt.ist.socialsoftware.mono2micro.functionality.domain.LocalTransaction;
import pt.ist.socialsoftware.mono2micro.fileManager.GridFsService;
import pt.ist.socialsoftware.mono2micro.log.domain.AccessesSciPyLog;
import pt.ist.socialsoftware.mono2micro.log.domain.accessesSciPyOperations.*;
import pt.ist.socialsoftware.mono2micro.log.repository.LogRepository;
import pt.ist.socialsoftware.mono2micro.log.service.AccessesSciPyLogService;
import pt.ist.socialsoftware.mono2micro.metrics.decompositionService.AccessesSciPyMetricService;
import pt.ist.socialsoftware.mono2micro.representation.domain.AccessesRepresentation;
import pt.ist.socialsoftware.mono2micro.representation.service.RepresentationService;
import pt.ist.socialsoftware.mono2micro.similarity.repository.AccessesSciPySimilarityRepository;
import pt.ist.socialsoftware.mono2micro.strategy.domain.AccessesSciPyStrategy;
import pt.ist.socialsoftware.mono2micro.strategy.repository.AccessesSciPyStrategyRepository;
import pt.ist.socialsoftware.mono2micro.utils.Utils;

import javax.management.openmbean.KeyAlreadyExistsException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static pt.ist.socialsoftware.mono2micro.representation.domain.AccessesRepresentation.ACCESSES;

@Service
public class AccessesSciPyDecompositionService {
    @Autowired
    AccessesSciPyStrategyRepository strategyRepository;

    @Autowired
    SimilarityRepository similarityRepository;

    @Autowired
    AccessesSciPySimilarityRepository accessesSciPySimilarityRepository;

    @Autowired
    SciPyClusteringAlgorithmService clusteringService;

    @Autowired
    AccessesSciPyDecompositionRepository decompositionRepository;

    @Autowired
    AccessesSciPyLogService accessesSciPyLogService;

    @Autowired
    LogRepository logRepository;

    @Autowired
    AccessesSciPyMetricService metricService;

    @Autowired
    FunctionalityService functionalityService;

    @Autowired
    FunctionalityRepository functionalityRepository;

    @Autowired
    RepresentationService representationService;

    @Autowired
    GridFsService gridFsService;

    public void createDecomposition(String similarityName, String cutType, float cutValue) throws Exception {
        AccessesSciPySimilarity similarity = accessesSciPySimilarityRepository.findByName(similarityName);
        AccessesSciPyStrategy strategy = strategyRepository.findByName(similarity.getStrategy().getName());
        clusteringService.createDecomposition(strategy, similarity, cutType, cutValue);
    }

    public void createExpertDecomposition(String similarityName, String expertName, Optional<MultipartFile> expertFile) throws Exception {
        AccessesSciPySimilarity similarity = accessesSciPySimilarityRepository.findByName(similarityName);
        AccessesSciPyStrategy strategy = strategyRepository.findByName(similarity.getStrategy().getName());
        clusteringService.createExpertDecomposition(strategy, similarity, expertName, expertFile);
    }

    public AccessesSciPyDecomposition updateOutdatedFunctionalitiesAndMetrics(String decompositionName) throws Exception {
        AccessesSciPyDecomposition decomposition = decompositionRepository.findByName(decompositionName);
        AccessesSciPySimilarity similarity = (AccessesSciPySimilarity) decomposition.getSimilarity();
        AccessesRepresentation representation = (AccessesRepresentation) similarity.getStrategy().getCodebase().getRepresentationByType(ACCESSES);
        if (!decomposition.isOutdated())
            return decomposition;

        functionalityService.setupFunctionalities(
                decomposition,
                representationService.getRepresentationFileAsInputStream(representation.getName()),
                representation.getProfile(similarity.getProfile()),
                similarity.getTracesMaxLimit(),
                similarity.getTraceType(),
                false);

        metricService.calculateMetrics(decomposition);
        decomposition.setOutdated(false);

        decompositionRepository.save(decomposition);
        return decomposition;
    }

    public void snapshotDecomposition(String decompositionName) throws Exception {
        AccessesSciPyDecomposition decomposition = updateOutdatedFunctionalitiesAndMetrics(decompositionName);
        AccessesSciPySimilarity similarity = (AccessesSciPySimilarity) decomposition.getSimilarity();
        String snapshotName = decomposition.getName() + " SNAPSHOT";

        // Find unused name
        if (similarity.getDecompositionByName(snapshotName) != null) {
            int i = 1;
            do {i++;} while (similarity.getDecompositionByName(snapshotName + "(" + i + ")") != null);
            snapshotName = snapshotName + "(" + i + ")";
        }
        AccessesSciPyDecomposition snapshotDecomposition = new AccessesSciPyDecomposition(decomposition);
        snapshotDecomposition.setName(snapshotName);

        // Duplicate functionalities for the new decomposition (also includes duplicating respective redesigns)
        for (Functionality functionality : decomposition.getFunctionalities().values()) {
            Functionality snapshotFunctionality = new Functionality(snapshotDecomposition.getName(), functionality);
            snapshotFunctionality.setFunctionalityRedesignNameUsedForMetrics(functionality.getFunctionalityRedesignNameUsedForMetrics());

            for (String redesignName : functionality.getFunctionalityRedesigns().keySet()) {
                FunctionalityRedesign functionalityRedesign = functionalityService.getFunctionalityRedesign(functionality, redesignName);
                snapshotFunctionality.addFunctionalityRedesign(functionalityRedesign.getName(), snapshotFunctionality.getId() + functionalityRedesign.getName());
                functionalityService.saveFunctionalityRedesign(snapshotFunctionality, functionalityRedesign);
            }
            snapshotDecomposition.addFunctionality(snapshotFunctionality);
            functionalityRepository.save(snapshotFunctionality);
        }

        AccessesSciPyLog snapshotLog = new AccessesSciPyLog(snapshotDecomposition);
        snapshotDecomposition.setLog(snapshotLog);
        logRepository.save(snapshotLog);
        accessesSciPyLogService.saveGraphPositions(snapshotDecomposition, accessesSciPyLogService.getGraphPositions(decomposition));

        similarity.addDecomposition(snapshotDecomposition);
        AccessesSciPyStrategy strategy = strategyRepository.findByName(similarity.getStrategy().getName());
        strategy.addDecomposition(snapshotDecomposition);
        snapshotDecomposition.setStrategy(strategy);
        decompositionRepository.save(snapshotDecomposition);
        accessesSciPySimilarityRepository.save(similarity);
        strategyRepository.save(strategy);
    }

    public Utils.GetSerializableLocalTransactionsGraphResult getLocalTransactionGraphForFunctionality(String decompositionName, String functionalityName) throws JSONException, IOException {
        AccessesSciPyDecomposition decomposition = decompositionRepository.findByName(decompositionName);
        AccessesSciPySimilarity similarity = (AccessesSciPySimilarity) decomposition.getSimilarity();
        AccessesRepresentation representation = (AccessesRepresentation) similarity.getStrategy().getCodebase().getRepresentationByType(ACCESSES);

        DirectedAcyclicGraph<LocalTransaction, DefaultEdge> functionalityLocalTransactionsGraph = decomposition.getFunctionality(functionalityName)
                .createLocalTransactionGraphFromScratch(
                        representationService.getRepresentationFileAsInputStream(representation.getName()),
                        similarity.getTracesMaxLimit(),
                        similarity.getTraceType(),
                        decomposition.getEntityIDToClusterName());

        return Utils.getSerializableLocalTransactionsGraph(functionalityLocalTransactionsGraph);
    }

    public ArrayList<HashMap<String, String>> getSearchItems(String decompositionName) {
        ArrayList<HashMap<String, String>> searchItems = new ArrayList<>();
        AccessesSciPyDecomposition decomposition = decompositionRepository.findByName(decompositionName);

        decomposition.getClusters().values().forEach(cluster -> {
            HashMap<String, String> clusterItem = new HashMap<>();
            clusterItem.put("name", cluster.getName());
            clusterItem.put("type", "Cluster");
            clusterItem.put("id", cluster.getName());
            clusterItem.put("entities", Integer.toString(cluster.getElements().size()));
            clusterItem.put("funcType", ""); clusterItem.put("cluster", "");
            searchItems.add(clusterItem);

            cluster.getElements().forEach(entity -> {
                HashMap<String, String> entityItem = new HashMap<>();
                entityItem.put("name", entity.getName());
                entityItem.put("type", "Entity");
                entityItem.put("id", String.valueOf(entity.getId()));
                entityItem.put("entities", ""); entityItem.put("funcType", "");
                entityItem.put("cluster", cluster.getName());
                searchItems.add(entityItem);
            });
        });

        decomposition.getFunctionalities().values().forEach(functionality -> {
            HashMap<String, String> functionalityItem = new HashMap<>();
            functionalityItem.put("name", functionality.getName());
            functionalityItem.put("type", "Functionality");
            functionalityItem.put("id", functionality.getName());
            functionalityItem.put("entities", Integer.toString(functionality.getEntities().size()));
            functionalityItem.put("funcType", functionality.getType().toString()); functionalityItem.put("cluster", "");
            searchItems.add(functionalityItem);
        });

        return searchItems;
    }

    public String getEdgeWeights(String decompositionName) throws JSONException, IOException {
        AccessesSciPyDecomposition decomposition = decompositionRepository.findByName(decompositionName);
        AccessesSciPySimilarity similarity = (AccessesSciPySimilarity) decomposition.getSimilarity();
        JSONArray copheneticDistances = new JSONArray(IOUtils.toString(gridFsService.getFile(similarity.getCopheneticDistanceName()), StandardCharsets.UTF_8));

        ArrayList<Short> entities = new ArrayList<>(decomposition.getEntityIDToClusterName().keySet());

        JSONArray edgesJSON = new JSONArray();
        int k = 0;
        for (int i = 0; i < entities.size(); i++) {
            for (int j = i + 1; j < entities.size(); j++) {
                short e1ID = entities.get(i);
                short e2ID = entities.get(j);

                JSONObject edgeJSON = new JSONObject();
                if (e1ID < e2ID) {
                    edgeJSON.put("e1ID", e1ID); edgeJSON.put("e2ID", e2ID);
                }
                else {
                    edgeJSON.put("e1ID", e2ID); edgeJSON.put("e1ID", e2ID);
                }
                edgeJSON.put("dist", copheneticDistances.getDouble(k));
                edgesJSON.put(edgeJSON);
                k++;
            }
        }

        // Get functionalities in common
        HashMap<String, JSONArray> entityRelations = new HashMap<>();
        for (Functionality functionality : decomposition.getFunctionalities().values()) {
            List<Short> functionalityEntities = new ArrayList<>(functionality.getEntities().keySet());
            for (int i = 0; i < functionalityEntities.size(); i++)
                for (int j = i + 1; j < functionalityEntities.size(); j++) {
                    JSONArray relatedFunctionalities = entityRelations.get(edgeId(functionalityEntities.get(i), functionalityEntities.get(j)));
                    if (relatedFunctionalities == null) {
                        relatedFunctionalities = new JSONArray();
                        relatedFunctionalities.put(functionality.getName());
                        entityRelations.put(edgeId(functionalityEntities.get(i), functionalityEntities.get(j)), relatedFunctionalities);
                    }
                    else relatedFunctionalities.put(functionality.getName());
                }
        }

        JSONArray filteredEdgesJSON = new JSONArray();
        for (int i = 0; i < edgesJSON.length(); i++) {
            JSONObject edgeJSON = edgesJSON.getJSONObject(i);
            JSONArray relatedFunctionalities = entityRelations.get(edgeId(edgeJSON.getInt("e1ID"), edgeJSON.getInt("e2ID")));

            if (relatedFunctionalities != null) {
                edgeJSON.put("functionalities", relatedFunctionalities);
                filteredEdgesJSON.put(edgeJSON);
            }
        }

        return filteredEdgesJSON.toString();
    }

    private String edgeId(int node1, int node2) {
        if (node1 < node2)
            return node1 + "&" + node2;
        return node2 + "&" + node1;
    }

    public Map<String, Cluster> mergeClustersOperation(AccessesSciPyDecomposition decomposition, String clusterName, String otherClusterName, String newName) {
        MergeOperation operation = new MergeOperation(decomposition, clusterName, otherClusterName, newName);

        mergeClusters(decomposition, clusterName, otherClusterName, newName);
        accessesSciPyLogService.addOperation(decomposition, operation);
        return decomposition.getClusters();
    }

    public void mergeClusters(AccessesSciPyDecomposition decomposition, String clusterName, String otherClusterName, String newName) {
        Cluster cluster1 = decomposition.getCluster(clusterName);
        Cluster cluster2 = decomposition.getCluster(otherClusterName);
        if (decomposition.clusterNameExists(newName) && !cluster1.getName().equals(newName) && !cluster2.getName().equals(newName))
            throw new KeyAlreadyExistsException("Cluster with name: " + newName + " already exists");

        Cluster mergedCluster = new AccessesSciPyCluster(newName);

        for(Element entity : cluster1.getElements()) {
            decomposition.getEntityIDToClusterName().replace(entity.getId(), mergedCluster.getName());
            functionalityService.deleteFunctionalities(removeFunctionalityWithEntity(decomposition, entity.getId()));
        }

        for(Element entity : cluster2.getElements()) {
            decomposition.getEntityIDToClusterName().replace(entity.getId(), mergedCluster.getName());
            functionalityService.deleteFunctionalities(removeFunctionalityWithEntity(decomposition, entity.getId()));
        }

        Set<Element> allEntities = new HashSet<>(cluster1.getElements());
        allEntities.addAll(cluster2.getElements());
        mergedCluster.setElements(allEntities);

        decomposition.transferCouplingDependencies(cluster1.getElementsIDs(), cluster1.getName(), mergedCluster.getName());
        decomposition.transferCouplingDependencies(cluster2.getElementsIDs(), cluster2.getName(), mergedCluster.getName());

        decomposition.removeCluster(clusterName);
        decomposition.removeCluster(otherClusterName);

        decomposition.addCluster(mergedCluster);
        decomposition.setOutdated(true);
        decompositionRepository.save(decomposition);
    }

    public Map<String, Cluster> renameClusterOperation(AccessesSciPyDecomposition decomposition, String clusterName, String newName) {
        RenameOperation operation = new RenameOperation(decomposition, clusterName, newName);

        renameCluster(decomposition, clusterName, newName);
        accessesSciPyLogService.addOperation(decomposition, operation);
        return decomposition.getClusters();
    }

    public void renameCluster(AccessesSciPyDecomposition decomposition, String clusterName, String newName) {
        if (decomposition.clusterNameExists(newName) && !clusterName.equals(newName)) throw new KeyAlreadyExistsException("Cluster with name: " + newName + " already exists");

        Cluster oldCluster = decomposition.getClusters().remove(clusterName);
        oldCluster.setName(newName);
        decomposition.addCluster(oldCluster);

        // Change coupling dependencies
        decomposition.getClusters().forEach((s, c) -> {
            AccessesSciPyCluster cluster = (AccessesSciPyCluster) c;
            Set<Short> dependencies = cluster.getCouplingDependencies().get(clusterName);
            if (dependencies != null) {cluster.getCouplingDependencies().remove(clusterName); cluster.addCouplingDependencies(newName, dependencies);}
        });

        // Change EntityIDtoClusterName cluster names
        decomposition.getEntityIDToClusterName().forEach((entityID, cName) -> {
            if (cName.equals(clusterName))
                decomposition.getEntityIDToClusterName().put(entityID, newName);
        });

        // Change functionalities
        decomposition.getFunctionalities().forEach((s, functionality) -> {
            Set<Short> entities = functionality.getEntitiesPerCluster().get(clusterName);
            if (entities != null) {
                functionality.getEntitiesPerCluster().remove(clusterName); functionality.getEntitiesPerCluster().put(newName, entities);

                functionality.getFunctionalityRedesigns().keySet().forEach(functionalityRedesignName -> {
                    try {
                        FunctionalityRedesign functionalityRedesign = functionalityService.getFunctionalityRedesign(functionality, functionalityRedesignName);
                        functionalityRedesign.getRedesign().forEach(localTransaction -> {
                            if (localTransaction.getClusterName().equals(clusterName)) {
                                localTransaction.setClusterName(newName);
                                localTransaction.setName(localTransaction.getId() + ": " + newName);
                            }
                        });
                        functionalityService.updateFunctionalityRedesign(functionality, functionalityRedesign);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            functionalityRepository.save(functionality);
        });

        decompositionRepository.save(decomposition);
    }

    public Map<String, Cluster> splitClusterOperation(AccessesSciPyDecomposition decomposition, String clusterName, String newName, String entitiesString) {
        SplitOperation operation = new SplitOperation(decomposition, clusterName, newName, entitiesString);

        splitCluster(decomposition, clusterName, newName, entitiesString);
        accessesSciPyLogService.addOperation(decomposition, operation);
        return decomposition.getClusters();
    }

    public void splitCluster(AccessesSciPyDecomposition decomposition, String clusterName, String newName, String entitiesString) {
        String[] entities = entitiesString.split(",");
        if (decomposition.clusterNameExists(newName)) throw new KeyAlreadyExistsException("Cluster with name: " + newName + " already exists");

        Cluster currentCluster = decomposition.getCluster(clusterName);
        Cluster newCluster = new AccessesSciPyCluster(newName);

        for (String stringifiedEntityID : entities) {
            Element entity = currentCluster.getElementByID(Short.parseShort(stringifiedEntityID));

            if (entity != null) {
                newCluster.addElement(entity);
                currentCluster.removeElement(entity);
                decomposition.getEntityIDToClusterName().replace(entity.getId(), newCluster.getName());
                functionalityService.deleteFunctionalities(removeFunctionalityWithEntity(decomposition, entity.getId()));
            }
        }
        decomposition.transferCouplingDependencies(newCluster.getElementsIDs(), currentCluster.getName(), newCluster.getName());
        decomposition.addCluster(newCluster);
        decomposition.setOutdated(true);
        decompositionRepository.save(decomposition);
    }

    public Map<String, Cluster> transferEntitiesOperation(AccessesSciPyDecomposition decomposition, String fromClusterName, String toClusterName, String entitiesString) {
        TransferOperation operation = new TransferOperation(decomposition, fromClusterName, toClusterName, entitiesString);

        transferEntities(decomposition, fromClusterName, toClusterName, entitiesString);
        accessesSciPyLogService.addOperation(decomposition, operation);
        return decomposition.getClusters();
    }

    public void transferEntities(AccessesSciPyDecomposition decomposition, String fromClusterName, String toClusterName, String entitiesString) {
        Cluster fromCluster = decomposition.getCluster(fromClusterName);
        Cluster toCluster = decomposition.getCluster(toClusterName);
        Set<Short> entities = Arrays.stream(entitiesString.split(",")).map(Short::valueOf).collect(Collectors.toSet());

        for (Short entityID : entities) {
            Element entity = fromCluster.getElementByID(entityID);
            if (entity != null) {
                toCluster.addElement(entity);
                fromCluster.removeElement(entity);
                decomposition.getEntityIDToClusterName().replace(entityID, toCluster.getName());
                functionalityService.deleteFunctionalities(removeFunctionalityWithEntity(decomposition, entityID));
            }
        }
        decomposition.transferCouplingDependencies(entities, fromClusterName, toClusterName);
        decomposition.setOutdated(true);
        decompositionRepository.save(decomposition);
    }

    public Map<String, Cluster> formClusterOperation(String decompositionName, String newName, Map<String, List<Short>> entities) {
        AccessesSciPyDecomposition decomposition = decompositionRepository.findByName(decompositionName);
        FormClusterOperation operation = new FormClusterOperation(decomposition, newName, entities);

        formCluster(decomposition, newName, entities);
        accessesSciPyLogService.addOperation(decomposition, operation);
        return decomposition.getClusters();
    }

    public void formCluster(AccessesSciPyDecomposition decomposition, String newName, Map<String, List<Short>> entities) {
        List<Short> entitiesIDs = entities.values().stream().flatMap(Collection::stream).collect(Collectors.toList());

        if (decomposition.clusterNameExists(newName)) {
            Cluster previousCluster = decomposition.getClusters().values().stream().filter(cluster -> cluster.getName().equals(newName)).findFirst()
                    .orElseThrow(() -> new KeyAlreadyExistsException("Cluster with name: " + newName + " already exists"));
            if (!entitiesIDs.containsAll(previousCluster.getElementsIDs()))
                throw new KeyAlreadyExistsException("Cluster with name: " + newName + " already exists");
        }

        Cluster newCluster = new AccessesSciPyCluster(newName);

        for (Short entityID : entitiesIDs) {
            Cluster currentCluster = decomposition.getClusters().values().stream()
                    .filter(cluster -> cluster.containsElement(entityID)).findFirst()
                    .orElseThrow(() -> new RuntimeException("No cluster contains entity " + entityID));

            Element entity = currentCluster.getElementByID(entityID);
            newCluster.addElement(entity);
            currentCluster.removeElement(entityID);
            decomposition.getEntityIDToClusterName().replace(entityID, newCluster.getName());
            functionalityService.deleteFunctionalities(removeFunctionalityWithEntity(decomposition, entityID));
            decomposition.transferCouplingDependencies(Collections.singleton(entityID), currentCluster.getName(), newCluster.getName());
            if (currentCluster.getElements().size() == 0)
                decomposition.removeCluster(currentCluster.getName());
        }
        decomposition.addCluster(newCluster);
        decomposition.setOutdated(true);
        decompositionRepository.save(decomposition);
    }

    public List<Functionality> removeFunctionalityWithEntity(AccessesSciPyDecomposition decomposition, short entityID) {
        Map<String, Functionality> newFunctionalities = new HashMap<>();
        List<Functionality> toDelete = new ArrayList<>();
        decomposition.getFunctionalities().forEach((name, functionality) -> {
            if (functionality.containsEntity(entityID))
                toDelete.add(functionality);
            else newFunctionalities.put(name, functionality);
        });
        decomposition.setFunctionalities(newFunctionalities);
        return toDelete;
    }

    public void deleteDecompositionProperties(AccessesSciPyDecomposition decomposition) {
        accessesSciPyLogService.deleteDecompositionLog(decomposition.getLog());
        functionalityService.deleteFunctionalities(decomposition.getFunctionalities().values());
        gridFsService.deleteFile(decomposition.getName() + "_refactorization");
        Similarity similarity = decomposition.getSimilarity();
        similarity.removeDecomposition(decomposition.getName());
        similarityRepository.save(similarity);
    }
}