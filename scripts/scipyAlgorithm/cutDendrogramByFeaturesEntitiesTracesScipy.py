import numpy as np
from scipy.cluster import hierarchy
from sklearn import metrics
import json


def cutDendrogramByFeaturesEntitiesTraces(codebasesPath, codebaseName, dendrogramName, graphName, cutType, cutValue):
    with open(codebasesPath + codebaseName + "/entities_traces_embeddings.json") as f:
        entities_traces_embeddings = json.load(f)

    with open(codebasesPath + codebaseName + "/datafile.json") as f:
        features_entities_accesses = json.load(f)

    with open(codebasesPath + codebaseName + "/translation.json") as f:
        translation_json = json.load(f)

    totalNumberOfEntities = len(translation_json.keys())
    names = []
    ids = []
    vectors = []
    for cls in entities_traces_embeddings['traces']:
        names += [cls['name']]
        vectors += [cls['codeVector']]

    matrix = np.array(vectors)
    linkageType = entities_traces_embeddings['linkageType']

    hierarc = hierarchy.linkage(y=matrix, method=linkageType)

    if cutType == "H":
        cut = hierarchy.cut_tree(hierarc, height=cutValue)
    elif cutType == "N":
        cut = hierarchy.cut_tree(hierarc, n_clusters=cutValue)

    clusters = {}
    for i in range(len(cut)):
        if str(cut[i][0]) in clusters.keys():
            clusters[str(cut[i][0])] += [i]
        else:
            clusters[str(cut[i][0])] = [i]

    entities_clusters_accesses = {}
    for entity in range(1, totalNumberOfEntities + 1):
        entities_clusters_accesses[entity] = {}
        for cluster in clusters.keys():
            entities_clusters_accesses[entity][cluster] = { "R" : 0, "W" : 0}

    for cluster in clusters.keys():

        for idx in clusters[cluster]:
            feature = names[idx]

            if feature in features_entities_accesses.keys():
                accesses = features_entities_accesses[feature]['t'][0]['a']

                for access in accesses:
                    access_type = access[0]
                    entity = access[1]
                    entities_clusters_accesses[entity][cluster][access_type] += 1

    entities_cluster = {}
    for entity in entities_clusters_accesses.keys():
        max_nbr_accesses = 0
        attr_cluster = "0"

        for cluster in entities_clusters_accesses[entity].keys():
            nbr_accesses = entities_clusters_accesses[entity][cluster]["R"] + entities_clusters_accesses[entity][cluster]["W"]

            if nbr_accesses > max_nbr_accesses:
                max_nbr_accesses = nbr_accesses
                attr_cluster = cluster

        if attr_cluster in entities_cluster.keys():
            entities_cluster[attr_cluster] += [entity]
        else:
            entities_cluster[attr_cluster] = [entity]

    for cluster in clusters.keys():
        if cluster not in entities_cluster.keys():
            entities_cluster[cluster] = []

    nodes = hierarchy.fcluster(hierarc, len(clusters), criterion="maxclust")
    try:
        silhouetteScore = metrics.silhouette_score(matrix, nodes)
    except:
        silhouetteScore = 0

    clustersJSON = {"silhouetteScore": "{0:.2f}".format(silhouetteScore), "clusters": entities_cluster}

    with open(codebasesPath + codebaseName + "/" + dendrogramName + "/" + graphName + "/clusters.json", 'w') as outfile:
        outfile.write(json.dumps(clustersJSON, indent=4))