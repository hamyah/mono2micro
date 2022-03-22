import numpy as np
from scipy.cluster import hierarchy
import json
import matplotlib
matplotlib.use('agg')
import matplotlib.pyplot as plt


def createDendrogramByFeaturesScipy(codebasesPath, codebaseName, dendrogramName):
    with open(codebasesPath + codebaseName + "/features_embeddings.json") as f:
        features_embeddings = json.load(f)

    names = []
    vectors = []
    for feature in features_embeddings['features']:
        names += [feature['signature'].split("(")[0].split(".")[-1]]
        vectors += [feature['codeVector']]

    matrix = np.array(vectors)

    linkageType = features_embeddings['linkageType']

    hierarc = hierarchy.linkage(y=matrix, method=linkageType)

    fig = plt.figure(figsize=(25, 10))

    hierarchy.dendrogram(hierarc, labels=names, distance_sort='descending')
    plt.savefig(codebasesPath + codebaseName + "/" + dendrogramName + "/dendrogramImage.png", format="png",
                bbox_inches='tight')
