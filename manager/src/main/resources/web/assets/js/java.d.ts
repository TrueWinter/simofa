/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2024-07-15 18:51:07.

export interface Account {
    id: string;
    username: string;
}

export interface Website {
    id: string;
    name: string;
    dockerImage: string;
    memory: number;
    cpu: number;
    gitUrl: string;
    gitBranch: string;
    gitCredential: GitCredential;
    buildCommand: string;
    deploymentCommand: string;
    deploymentFailedCommand: string;
    deploymentServer: string;
    deployToken: string;
}

export interface Template {
    id: string;
    name: string;
    template: string;
}

export interface Container {
    id: string;
    name: string;
    state: string;
    status: string;
}

export interface Image {
    name: string;
    size: string;
}

export interface GitCredential {
    id: string;
    username: string;
}
