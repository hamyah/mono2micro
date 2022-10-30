import React, {useEffect, useState} from 'react';
import { APIService } from '../../services/APIService';
import Row from 'react-bootstrap/Row';
import Breadcrumb from 'react-bootstrap/Breadcrumb';
import {useParams} from "react-router-dom";
import {SciPyDecompositionForm} from "./forms/SciPyDecompositionForm";
import {toast, ToastContainer} from "react-toastify";
import {SIMILARITY_MATRIX_SCIPY} from "../../models/similarity/SimilarityMatrixSciPy";

export const Decompositions = () => {
    let { codebaseName, strategyName, similarityName } = useParams();
    const [similarity, setSimilarity] = useState({});
    const [decompositions, setDecompositions] = useState([]);

    //Executed on mount
    useEffect(() => {
        const service = new APIService();
        service.getSimilarity(similarityName).then(response => {
            setSimilarity(response);
            loadDecompositions();
        });
    }, []);

    function loadDecompositions() {
        const service = new APIService();
        const toastId = toast.loading("Fetching Decompositions...");
        service.getDecompositions(
            similarityName
        ).then(response => {
            setDecompositions(response);
            toast.update(toastId, {type: toast.TYPE.SUCCESS, render: "Decompositions Loaded.", isLoading: false});
            setTimeout(() => {toast.dismiss(toastId)}, 1000);
        }).catch(() => {
            toast.update(toastId, {type: toast.TYPE.ERROR, render: "Error Loading Decompositions.", isLoading: false});
        });
    }

    function handleDeleteDecomposition(decompositionName) {
        const toastId = toast.loading("Deleting " + decompositionName + "...");
        const service = new APIService();
        service.deleteDecomposition(decompositionName).then(() => {
            loadDecompositions();
            toast.update(toastId, {type: toast.TYPE.SUCCESS, render: "Decomposition deleted.", isLoading: false});
            setTimeout(() => {toast.dismiss(toastId)}, 1000);
        }).catch(() => {
            toast.update(toastId, {type: toast.TYPE.ERROR, render: "Error deleting " + decompositionName + ".", isLoading: false});
        });
    }

    function renderBreadCrumbs() {
        return (
            <Breadcrumb>
                <Breadcrumb.Item href="/">
                    Home
                </Breadcrumb.Item>
                <Breadcrumb.Item href="/codebases">
                    Codebases
                </Breadcrumb.Item>
                <Breadcrumb.Item href={`/codebases/${codebaseName}`}>
                    {codebaseName}
                </Breadcrumb.Item>
                <Breadcrumb.Item href={`/codebases/${codebaseName}/${strategyName}/similarity`}>
                    {strategyName}
                </Breadcrumb.Item>
                <Breadcrumb.Item active>
                    {similarityName}
                </Breadcrumb.Item>
            </Breadcrumb>
        );
    }

    return (
        <div style={{ paddingLeft: "2rem" }}>
            <ToastContainer
                position="top-center"
                theme="colored"
            />

            {renderBreadCrumbs()}

            <h4 style={{color: "#666666"}}>
                Decomposition Creation Method
            </h4>

            {similarity.type === SIMILARITY_MATRIX_SCIPY &&
                <SciPyDecompositionForm loadDecompositions={loadDecompositions}/>
            }

            {decompositions.length !== 0 &&
                <h4 style={{color: "#666666", marginTop: "16px"}}>
                    Decompositions
                </h4>
            }

            <Row className={"d-flex flex-wrap mw-100"} style={{gap: '1rem 1rem'}}>
                {decompositions.map(decomposition => decomposition.printCard(loadDecompositions, handleDeleteDecomposition))}
            </Row>
        </div>
    );
}