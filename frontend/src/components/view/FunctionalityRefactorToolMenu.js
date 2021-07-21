import React from 'react';
import {RepositoryService} from '../../services/RepositoryService';
import BootstrapTable from 'react-bootstrap-table-next';
import Button from '@material-ui/core/Button';
import CircularProgress from '@material-ui/core/CircularProgress';
import Slider from '@material-ui/core/Slider';
import Divider from '@material-ui/core/Divider';
import GrainIcon from '@material-ui/icons/Grain';

export const refactorToolHelp = (
<div>
    A brute-force heuristic algorithm to estimate the best Saga redesign for each one of the controllers of a given decomposition.

    <br></br>

    <b>Data dependence:</b> the number of previous entity accesses that will be taken into account when deciding if two invocations can be merged.
    If set to 0, every Read access before the invocation being analyzed until the previous invocation of the same cluster, will be considered as a data dependence of a Write in the first.
</div>
);

export class FunctionalityRefactorToolMenu extends React.Component{

    constructor(props) {
        super(props);

        this.state = {
            waitingResponse: false,
            controllers: [],
            dataDependenceThreshold: 0,
            timeoutSecs: null,
            minimizeSumOfComplexities: false,
            refactorization: null,
            refactorizationRows: [],
            refactorizationColumns: [
                {
                    dataField: 'controller',
                    text: 'Controller',
                    sort: true,
                }, {
                    dataField: 'status',
                    text: 'Status',
                    sort: true,
                }, {
                    dataField: 'error',
                    text: 'Error',
                    sort: false,
                    hidden: true,
                }, {
                    dataField: 'functionalityComplexity',
                    text: 'Functionality Complexity',
                    sort: true,
                    hidden: true
                }, {
                    dataField: 'systemComplexity',
                    text: 'System Complexity',
                    sort: true,
                    hidden: true,
                }, {
                    dataField: 'invocations',
                    text: 'Invocations',
                    sort: true,
                    hidden: true
                }, {
                    dataField: 'accesses',
                    text: 'Accesses',
                    sort: true,
                    hidden: true
                }, {
                    dataField: 'sagaOrchestrator',
                    text: 'Orch.',
                    sort: true
                }, {
                    dataField: 'sagaFunctionalityComplexity',
                    text: 'FRC',
                    sort: true
                },  {
                    dataField: 'sagaSystemComplexity',
                    text: 'SAC',
                    sort: true
                },  {
                    dataField: 'sagaInvocations',
                    text: 'Invocations',
                    sort: true
                },  {
                    dataField: 'sagaAccesses',
                    text: 'Accesses',
                    sort: true
                },  {
                    dataField: 'functionalityComplexityReduction',
                    text: 'FRC Reduction',
                    sort: true
                },  {
                    dataField: 'systemComplexityReduction',
                    text: 'SAC Reduction',
                    sort: true
                },  {
                    dataField: 'invocationMerges',
                    text: 'Merges',
                    sort: true
                }
            ],
            functionalitiesRedesignRows: [],
            functionalityRedesignColumns: [
                {
                    dataField: 'cluster',
                    text: 'Cluster',
                    sort: false,
                }, {
                    dataField: 'entity_accesses',
                    text: 'Entity Accesses',
                    sort: false,
                }, 
            ]
        };

        this.requestCodebaseRefactor = this.requestCodebaseRefactor.bind(this);
        this.viewCodebaseRefactor = this.viewCodebaseRefactor.bind(this);
    }

    handleChangeThreshold(event, newValue) {
        this.setState({dataDependenceThreshold: newValue});
    }

    handleChangeTimeout(event, newValue) {
        this.setState({timeoutSecs: newValue});
    }

    handleDataParsing(data) {
        var refactorizationRows = []

        try {
            for(const [key, controller] of Object.entries(data['controllers'])) {
                var controllerData = {};
                if(controller['status'] === "COMPLETED") {
                    controllerData = {
                        name: key,
                        functionalityComplexity: controller['monolith']['complexity_metrics']['functionality_complexity'],
                        systemComplexity: controller['monolith']['complexity_metrics']['system_complexity'],
                        invocations: controller['monolith']['complexity_metrics']['invocations_count'] === undefined ? 0 : controller['monolith']['complexity_metrics']['invocations_count'],
                        accesses: controller['monolith']['complexity_metrics']['accesses_count'] === undefined ? 0 : controller['monolith']['complexity_metrics']['accesses_count'],
                        sagaOrchestrator: controller['refactor'] === undefined ? 0 : controller['refactor']['orchestrator']['name'],
                        sagaFunctionalityComplexity: controller['refactor'] === undefined ? 0 : controller['refactor']['complexity_metrics']['functionality_complexity'],
                        sagaSystemComplexity: controller['refactor'] === undefined ? 0 : controller['refactor']['complexity_metrics']['system_complexity'],
                        sagaInvocations: controller['refactor'] === undefined ? 0 : controller['refactor']['complexity_metrics']['invocations_count'],
                        sagaAccesses: controller['refactor'] === undefined ? 0 : controller['refactor']['complexity_metrics']['accesses_count'],
                        functionalityComplexityReduction: controller['refactor'] === undefined ? 0 : controller['refactor']['execution_metrics']['functionality_complexity_reduction'],
                        systemComplexityReduction: controller['refactor'] === undefined ? 0 : controller['refactor']['execution_metrics']['system_complexity_reduction'],
                        invocationMerges: controller['refactor'] === undefined ? 0 : controller['refactor']['execution_metrics']['invocation_merges'],
                        callGraph: controller['refactor'] === undefined ? 0 : controller['refactor']['call_graph'],
                    };

                    this.handleRedesignDataParsing(key, controller['refactor']["call_graph"])
                }


                controllerData["controller"] = key;
                controllerData["error"] = controller['error'];
                controllerData["status"] = controller['status'];

                refactorizationRows = refactorizationRows.concat(controllerData)
            }

            this.setState({
                refactorizationRows: refactorizationRows,
            });
        }
        catch (error) {
            console.log(error)
        }
    }

    handleRedesignDataParsing(name, data) {
        var functionalitiesRedesignRows = this.state.functionalitiesRedesignRows;

        try {
            var invocations = [];

            for(var i=0; i < data.length; i++) {
                var invocationAccesses = "";
                
                for(var j=0; j<data[i]["accesses"].length; j++) {
                    invocationAccesses = invocationAccesses.concat(data[i]["accesses"][j]["type"], " -> ", data[i]["accesses"][j]["entity_id"], "    |    ");
                }

                console.log(invocationAccesses)

                invocations = invocations.concat(
                    {
                        cluster: data[i]['cluster_id'],
                        entity_accesses: invocationAccesses,
                    }
                )
            }

            functionalitiesRedesignRows[name] = invocations
            this.setState({
                functionalitiesRedesignRows: functionalitiesRedesignRows,
            });
        }
        catch (error) {
            console.log(error)
        }
    }

    viewCodebaseRefactor(){
        const service = new RepositoryService();

        this.setState({
            waitingResponse: true,
        });

        service.viewRefactor(this.props.codebaseName, this.props.dendrogramName, this.props.decompositionName)
            .then(response => {
                this.setState({
                    waitingResponse: false,
                    refactorizationExists: true,
                });
                this.handleDataParsing(response.data)
            }).catch(() => {
                this.setState({
                    error: true,
                    errorMessage: 'ERROR: Failed to view refactorization of the codebase.',
                    waitingResponse: false,
                    refactorizationExists: false,
                });
            }
        );
    }

    requestCodebaseRefactor(newRefactor){
        const service = new RepositoryService();

        this.setState({
            waitingResponse: true,
        });

        service.refactorCodebase(this.props.codebaseName, this.props.dendrogramName, this.props.decompositionName,
            this.state.controllers, Number(this.state.dataDependenceThreshold), this.state.minimizeSumOfComplexities, Number(this.state.timeoutSecs), newRefactor)
            .then(response => {
                this.setState({
                    waitingResponse: false,
                    refactorizationExists: true,
                });
                this.handleDataParsing(response.data)
            }).catch(() => {
                this.setState({
                    error: true,
                    errorMessage: 'ERROR: Failed to request refactorization of the codebase.',
                    waitingResponse: false,
                });
            }
        );
    }

    componentDidMount() {
        this.viewCodebaseRefactor()
    }

    render() {
        let waitingResponse = this.state.waitingResponse
        let refactorizationExists = this.state.refactorizationExists

        const rowEvents = {
            onMouseEnter: (e, row, rowIndex) => {
                document.body.style.cursor = 'pointer';
            },
            onMouseLeave: (e, row, rowIndex) => {
                document.body.style.cursor = 'default';
            }
        };

        const expandRow = {
            renderer: row => (
                <div>
                    {row["error"] != "" ? <div><b>Error:</b> {row["error"]}</div> : ""}

                    <div style={
                        {
                            margin: "auto",
                            width: "50%",
                            textAlign: "center",
                        }
                    }>
                        {
                            row['status'] === "COMPLETED" ? <BootstrapTable
                                    bootstrap4
                                    keyField='controller'
                                    data={this.state.functionalitiesRedesignRows[row["name"]]}
                                    columns={this.state.functionalityRedesignColumns}
                                    expandRow={ expandRow }
                                    caption={"Saga redesign proposed:"}
                                    bordered={false}
                                    hover={true}
                                    rowEvents={rowEvents}
                            /> : ""
                        }
                    </div>

                    <br></br>

                    <h6><b>Initial monolith decomposition metrics:</b></h6>
                    <div>Functionality Redesign Complexity: <b>{row["functionalityComplexity"]}</b></div>
                    <div>System Added Complexity: <b>{row["systemComplexity"]}</b></div>
                    <div>Cluster Invocations: <b>{row["invocations"]}</b></div>
                    <div>Entity Accesses: <b>{row["accesses"]}</b></div>
                </div>
            )
        };

        const defaultSorted = [{
            dataField: 'status',
            order: 'asc' // desc or asc
          }];          

        return (
            <div>
                <div style={
                    {
                        margin: "auto",
                        width: "50%",
                        fontSize: "30px",
                        fontWeight: "600",
                        padding: "10px",
                        paddingBottom: "5px",
                        textAlign: "center",
                    }
                }>
                    <div><GrainIcon style={{ color: "#007acc", fontSize: 70 }}></GrainIcon></div>
                    Refactorization Tool
                </div>
                <div style={
                    {
                        margin: "auto",
                        width: "50%",
                        fontSize: "20px",
                        padding: "5px",
                        paddingBottom: "20px",
                        textAlign: "center",
                    }
                }>
                    Automatically compute microservice Saga orchestrations
                </div>

                <div style={
                    {
                        margin: "auto",
                        width: "60%",
                        fontSize: "16px",
                        padding: "5px",
                        paddingBottom: "40px",
                        textAlign: "center",
                    }
                }>
                    <div style={
                        {
                            margin: "auto",
                            marginBottom: "15px",
                            width: "70%",
                        }
                    }><Divider></Divider></div>
                    <p>The tool works by creating a Saga with each one of the clusters as orchestrator and then choosing the one that minimizes the migration cost.</p>
<p>During the redesign, the tool inserts one invocation of the orchestrator cluster between each invocation of two other clusters. 
After this, a recursive algorithm is executed, which iterates through each invocation in the callgraph and checks if it can be merged with the previous invocation of the same cluster.</p>
<p>Two invocations can be merged if there are no read accesses in the latest invocations of other clusters before the second one. 

The data dependance scope allows one to configure the maximum invocation distance for which a dependance will be considered. The value 0 considers all invocations 
between the one being analyzed and the last one from the same cluster.</p>
                    <div style={
                        {
                            margin: "auto",
                            marginTop: "15px",
                            width: "70%",
                        }
                    }><Divider></Divider></div>
                </div>
                <div style={
                    {
                        margin: "auto",
                        width: "30%",
                        border: "1px solid grey",
                        borderRadius: "10px",
                        padding: "10px",
                        textAlign: "center",
                        padding: "20px" 
                    }
                }>
                    <label>
                        Maximum Data Dependence Distance
                    </label>
                    

                    <div style={{width: "50%", marginLeft: "25%"}}>
                        <Slider
                            onChange={this.handleChangeThreshold.bind(this)}
                            defaultValue={0}
                            aria-labelledby="discrete-slider"
                            valueLabelDisplay="auto"
                            step={1}
                            marks
                            min={0}
                            max={5}
                        />
                    </div>

                    <br></br>

                    {/* <label>
                        Refactorization timeout (seconds):
                    </label>

                    <div style={{width: "50%", marginLeft: "25%"}}>
                        <Slider
                            onChange={this.handleChangeTimeout.bind(this)}
                            defaultValue={0}
                            aria-labelledby="continuous-slider"
                            valueLabelDisplay="auto"
                            step={10}
                            min={60}
                            max={1000}
                        />
                    </div> */}

                    <Button 
                        onClick={ () => this.requestCodebaseRefactor(true) } 
                        variant="contained" 
                        color="primary"
                    >
                        Start
                    </Button>
                </div>

                <br></br>

                {waitingResponse && 
                    <div style={
                        {
                            margin: "auto",
                            textAlign: "center",
                        }
                    }>
                        <CircularProgress />
                    </div>
                }

                {refactorizationExists &&
                    <div>
                        <Button 
                            onClick={ () => this.viewCodebaseRefactor() } 
                            variant="contained" 
                            color="primary"
                        >
                            Update table
                        </Button>

                        <br></br>
            
                        <BootstrapTable
                            bootstrap4
                            keyField='controller'
                            data={this.state.refactorizationRows}
                            columns={this.state.refactorizationColumns}
                            expandRow={ expandRow }
                            caption={"Results of last estimation for the valid Saga controllers:"}
                            bordered={false}
                            hover={true}
                            defaultSorted={defaultSorted}
                            rowEvents={rowEvents}
                        />
                    </div>
                }
            </div>
        )
    }
}
