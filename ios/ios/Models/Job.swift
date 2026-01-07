//
//  Job.swift
//  ios
//
//  Jenkins job model
//

import Foundation
import SwiftUI

struct Job: Codable, Identifiable, Equatable, Hashable {
    let name: String
    let url: String
    let color: String?
    let lastBuild: BuildReference?
    let lastSuccessfulBuild: BuildReference?
    let lastFailedBuild: BuildReference?
    let lastCompletedBuild: BuildReference?
    let buildable: Bool?
    let healthReport: [HealthReport]?
    
    var id: String { name }
    
    var status: JobStatus {
        guard let color = color else { return .unknown }
        switch color {
        case "blue", "blue_anime":
            return .success
        case "red", "red_anime":
            return .failure
        case "yellow", "yellow_anime":
            return .unstable
        case "grey", "grey_anime", "disabled", "disabled_anime":
            return .disabled
        case "aborted", "aborted_anime":
            return .aborted
        case "notbuilt", "notbuilt_anime":
            return .notBuilt
        default:
            return .unknown
        }
    }
    
    var isBuilding: Bool {
        color?.contains("_anime") ?? false
    }
    
    var healthScore: Int {
        healthReport?.first?.score ?? 0
    }
}

struct BuildReference: Codable, Equatable, Hashable {
    let number: Int
    let url: String
}

struct HealthReport: Codable, Equatable, Hashable {
    let description: String?
    let score: Int
}

enum JobStatus: String, CaseIterable {
    case success
    case failure
    case unstable
    case disabled
    case aborted
    case notBuilt
    case unknown
    
    var color: Color {
        switch self {
        case .success: return .green
        case .failure: return .red
        case .unstable: return .yellow
        case .disabled: return .gray
        case .aborted: return .gray
        case .notBuilt: return .gray
        case .unknown: return .gray
        }
    }
    
    var iconName: String {
        switch self {
        case .success: return "checkmark.circle.fill"
        case .failure: return "xmark.circle.fill"
        case .unstable: return "exclamationmark.triangle.fill"
        case .disabled: return "pause.circle.fill"
        case .aborted: return "stop.circle.fill"
        case .notBuilt: return "circle.dashed"
        case .unknown: return "questionmark.circle.fill"
        }
    }
}

// MARK: - API Response Wrappers

struct JobsResponse: Codable {
    let jobs: [Job]
}

