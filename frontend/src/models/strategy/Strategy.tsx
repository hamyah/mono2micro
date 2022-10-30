import Card from "react-bootstrap/Card";
import Button from "react-bootstrap/Button";
import React from "react";

export default class Strategy {
    codebaseName!: string;
    name!: string;
    algorithmType!: string;
    representationInformationTypes!: string[];

    constructor(strategy: any) {
        this.codebaseName = strategy.codebaseName;
        this.name = strategy.name;
        this.algorithmType = strategy.algorithmType;
        this.representationInformationTypes = strategy.representationInformationTypes;
    }

    // This function is used to be displayed in the context of the codebase
    printCard(handleDeleteStrategy: (strategy: Strategy) => void): JSX.Element {
        return <Card className={"text-center"} key={this.name} style={{width: '13rem'}}>
            <Card.Header>{this.name}</Card.Header>
            <Card.Body>
                <span>{this.algorithmType}<br/></span>
                {this.representationInformationTypes.map(rep => <span key={rep}>{rep}<br/></span> )}
                <Button
                    href={`/codebases/${this.codebaseName}/${this.name}/similarity`}
                    className="mb-2"
                >
                    Similarity Distance Generation
                </Button>
                <br/>
                <Button
                    href={`/codebases/${this.codebaseName}/${this.name}/recommendation`}
                    className="mb-2"
                >
                    Recommendation
                </Button>
                <br/>
                <Button
                    onClick={() => handleDeleteStrategy(this)}
                    className="mb-2"
                    variant={"danger"}
                >
                    Delete
                </Button>
            </Card.Body>
        </Card>;
    }
}