/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2024-08-15 19:31:22.

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
    gitCredentials: string;
    buildCommand: string;
    deployCommand: string;
    deployFailedCommand: string;
    deployServer: string;
    deployToken: string;
}

export interface GitCredential {
    id: string;
    username: string;
}

export interface DeployServer {
    id: string;
    name: string;
    url: string;
    key: string;
}

export interface SimofaLog {
    type: string;
    log: string;
    uuid: string;
    timestamp: number;
}

export interface Template {
    id: string;
    name: string;
    template: string;
}
