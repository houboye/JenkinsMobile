//
//  JenkinsView.swift
//  ios
//
//  Jenkins view model (not to be confused with SwiftUI View)
//

import Foundation

struct JenkinsView: Codable, Identifiable, Equatable {
    let name: String
    let url: String
    let jobs: [Job]?
    
    var id: String { name }
    
    var displayName: String {
        if name == "all" || name == "All" {
            return "所有"
        }
        return name
    }
}

// MARK: - API Response Wrappers

struct JenkinsRootResponse: Codable {
    let views: [JenkinsView]
    let primaryView: JenkinsView?
    let nodeDescription: String?
    let nodeName: String?
    let mode: String?
}

struct ViewDetailResponse: Codable {
    let name: String
    let url: String
    let jobs: [Job]
}

