import React from 'react';
import { TransactionOperationsMenu } from './TransactionOperationsMenu';
import { RepositoryService } from './../../services/RepositoryService';
import { VisNetwork } from '../util/VisNetwork';
import { DataSet } from 'vis';
import { views, types } from './ViewsMenu';

export const transactionViewHelp = (<div>
    Hover or double click cluster to see entities inside.<br />
    Hover or double click edge to see entities accessed.<br />
    Select cluster or edge for highlight.
    </div>);

const options = {
    height: "700",
    layout: {
        hierarchical: {
            direction: 'UD',
            nodeSpacing: 120
        }
    },
    edges: {
        smooth: false,
        arrows: {
          to: {
            enabled: true,
          }
        },
        scaling: {
            label: {
                enabled: true
            },
        },
        color: {
            color: "#2B7CE9",
            hover: "#2B7CE9",
            highlight: "#FFA500"
        }
    },
    nodes: {
        shape: 'ellipse',
        scaling: {
            label: {
                enabled: true
            },
        },
        color: {
            border: "#2B7CE9",
            background: "#D2E5FF",
            highlight: {
                background: "#FFA500",
                border: "#FFA500"
            }
        }
    },
    interaction: {
        hover: true
    },
    physics: {
        enabled: false
    }
};

export class TransactionView extends React.Component {
    constructor(props) {
        super(props);

        this.state = {
            dendrogramName: this.props.dendrogramName,
            graphName: this.props.graphName,
            graph: {},
            controller: {},
            controllers: [],
            controllerClusters: [],
            showGraph: false
        }

        this.handleControllerSubmit = this.handleControllerSubmit.bind(this);
        this.loadGraph = this.loadGraph.bind(this);
        this.createNode = this.createNode.bind(this);
        this.createEdge = this.createEdge.bind(this);
        this.handleSelectNode = this.handleSelectNode.bind(this);
    }

    componentDidMount() {
        const service = new RepositoryService();
        service.getControllers(this.state.dendrogramName).then(response => {
            this.setState({
                controllers: response.data
            });
        });
        service.getControllerClusters(this.state.dendrogramName, this.props.graphName).then(response => {
            this.setState({
                controllerClusters: response.data
            });
        });
    }

    handleControllerSubmit(value) {
        this.setState({
            controller: this.state.controllers.filter(c => c.name === value)[0],
            showGraph: true
        }, () => {
            this.loadGraph();
            }
        );
    }

    loadGraph() {
        const graph = {
            nodes: new DataSet(this.state.controllerClusters[this.state.controller.name].map(c => this.createNode(c))),
            edges: new DataSet(this.state.controllerClusters[this.state.controller.name].map(c => this.createEdge(c)))
        };
        graph.nodes.add({id: this.state.controller.name, title: this.state.controller.entities.join('<br>'), label: this.state.controller.name, level: 0, value: 1, type: types.CONTROLLER});

        this.setState({
            graph: graph
        });
    }

    createNode(cluster) {
        return {id: cluster.name, title: cluster.entities.map(e => e.name).join('<br>'), label: cluster.name, value: cluster.entities.length, level: 1, type: types.CLUSTER};
    }


    createEdge(cluster) {
        let entitiesTouched = cluster.entities.map(e => e.name).filter(e => this.state.controller.entities.includes(e));
        return {from: this.state.controller.name, to: cluster.name, label: entitiesTouched.length.toString(), title: entitiesTouched.join('<br>')};
    }

    handleSelectNode(nodeId) {

    }

    handleDeselectNode(nodeId) {

    }

    render() {
        let controllerClustersMap = Object.keys(this.state.controllerClusters).map(key => this.state.controllerClusters[key].length);
        let averageClustersAccessed = controllerClustersMap.reduce((a,b) => a + b, 0) / controllerClustersMap.length;

        return (
            <div>
                <TransactionOperationsMenu
                    handleControllerSubmit={this.handleControllerSubmit}
                    controllers={this.state.controllers}
                    controllerClusters={this.state.controllerClusters}
                />
                
                <div style={{width:'1000px' , height: '700px'}}>
                    <VisNetwork 
                        graph={this.state.graph}
                        options={options}
                        onSelection={this.handleSelectNode}
                        onDeselection={this.handleDeselectNode}
                        view={views.TRANSACTION} />
                </div>

                <div>
                    Number of Retrieved Controllers : {this.state.controllers.length}< br/>
                    Number of Controllers that access a single Cluster : {Object.keys(this.state.controllerClusters).filter(key => this.state.controllerClusters[key].length === 1).length}< br/>
                    Maximum number of Clusters accessed by a single Controller : {Math.max(...Object.keys(this.state.controllerClusters).map(key => this.state.controllerClusters[key].length))}< br/>
                    Average Number of Clusters accessed (Average number of microservices accessed during a transaction) : {averageClustersAccessed}
                </div>
            </div>
        );
    }
}