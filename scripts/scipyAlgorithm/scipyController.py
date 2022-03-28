from fastapi import APIRouter
from scipyAlgorithm.createDendrogram import createDendrogram as createDendrogramScipy
from scipyAlgorithm.createDendrogramByClassesScipy import createDendrogramByClassesScipy as createDendrogramByClassesScipy
from scipyAlgorithm.createDendrogramByFeaturesScipy import createDendrogramByFeaturesScipy as createDendrogramByFeaturesScipy
from scipyAlgorithm.cutDendrogram import cutDendrogram as cutDendrogramScipy
from scipyAlgorithm.cutDendrogramByClassesScipy import cutDendrogramByClasses as cutDendrogramByClassesScipy
from scipyAlgorithm.cutDendrogramByFeaturesScipy import cutDendrogramByFeatures as cutDendrogramByFeaturesScipy
from scipyAlgorithm.analyser import analyser as analyserScipy
import env

scipyRouter = APIRouter()


@scipyRouter.get("/scipy/{codebaseName}/{dendrogramName}/createDendrogram")
async def createDendrogram(codebaseName, dendrogramName):
    createDendrogramScipy(env.CODEBASES_PATH, codebaseName, dendrogramName)
    return {"codebaseName": codebaseName, "dendrogramName": dendrogramName, "operation": "createDendrogram"}


@scipyRouter.get("/scipy/{codebaseName}/{dendrogramName}/createDendrogram/classes")
async def createDendrogramByClasses(codebaseName, dendrogramName):
    createDendrogramByClassesScipy(env.CODEBASES_PATH, codebaseName, dendrogramName)
    return {"codebaseName": codebaseName, "dendrogramName": dendrogramName, "operation": "createDendrogram"}


@scipyRouter.get("/scipy/{codebaseName}/{dendrogramName}/createDendrogram/features")
async def createDendrogramByFeatures(codebaseName, dendrogramName):
    createDendrogramByFeaturesScipy(env.CODEBASES_PATH, codebaseName, dendrogramName)
    return {"codebaseName": codebaseName, "dendrogramName": dendrogramName, "operation": "createDendrogram"}


@scipyRouter.get("/scipy/{codebaseName}/{dendrogramName}/{graphName}/{cutType}/{cutValue}/cut")
async def cutDendrogram(codebaseName, dendrogramName, graphName, cutType, cutValue):
    cutDendrogramScipy(env.CODEBASES_PATH, codebaseName, dendrogramName, graphName, cutType, float(cutValue))
    return {"codebaseName": codebaseName, "dendrogramName": dendrogramName, "graphName": graphName,
            "cutType": cutType, "cutValue": cutValue, "operation": "cutDendrogram"}

@scipyRouter.get("/scipy/{codebaseName}/{dendrogramName}/{graphName}/{cutType}/{cutValue}/cut/classes")
async def cutDendrogramByClasses(codebaseName, dendrogramName, graphName, cutType, cutValue):
    cutDendrogramByClassesScipy(env.CODEBASES_PATH, codebaseName, dendrogramName, graphName, cutType, float(cutValue))
    return {"codebaseName": codebaseName, "dendrogramName": dendrogramName, "graphName": graphName,
            "cutType": cutType, "cutValue": cutValue, "operation": "cutDendrogram"}


@scipyRouter.get("/scipy/{codebaseName}/{dendrogramName}/{graphName}/{cutType}/{cutValue}/cut/features")
async def cutDendrogramByFeatures(codebaseName, dendrogramName, graphName, cutType, cutValue):
    cutDendrogramByFeaturesScipy(env.CODEBASES_PATH, codebaseName, dendrogramName, graphName, cutType, float(cutValue))
    return {"codebaseName": codebaseName, "dendrogramName": dendrogramName, "graphName": graphName,
            "cutType": cutType, "cutValue": cutValue, "operation": "cutDendrogram"}


@scipyRouter.get("/scipy/{codebaseName}/{totalNumberOfEntities}/analyser")
async def anayser(codebaseName, totalNumberOfEntities):
    analyserScipy(env.CODEBASES_PATH, codebaseName, int(totalNumberOfEntities))
    return {"codebaseName": codebaseName, "totalNumberOfEntities": totalNumberOfEntities, "operation": "analyser"}
