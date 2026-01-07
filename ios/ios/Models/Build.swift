//
//  Build.swift
//  ios
//
//  Jenkins build model
//

import Foundation
import SwiftUI

struct Build: Codable, Identifiable, Equatable {
    let number: Int
    let url: String
    let result: String?
    let timestamp: Int64?
    let duration: Int64?
    let displayName: String?
    let building: Bool?
    let description: String?
    let estimatedDuration: Int64?
    let fullDisplayName: String?
    
    var id: Int { number }
    
    var buildResult: BuildResult {
        guard let result = result else {
            return building == true ? .building : .unknown
        }
        return BuildResult(rawValue: result) ?? .unknown
    }
    
    var startDate: Date? {
        guard let timestamp = timestamp else { return nil }
        return Date(timeIntervalSince1970: Double(timestamp) / 1000)
    }
    
    var formattedDuration: String {
        guard let duration = duration else { return "-" }
        let seconds = duration / 1000
        if seconds < 60 {
            return "\(seconds)秒"
        } else if seconds < 3600 {
            let minutes = seconds / 60
            let secs = seconds % 60
            return "\(minutes)分\(secs)秒"
        } else {
            let hours = seconds / 3600
            let minutes = (seconds % 3600) / 60
            return "\(hours)小时\(minutes)分"
        }
    }
    
    var formattedTimestamp: String {
        guard let date = startDate else { return "-" }
        let formatter = RelativeDateTimeFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.unitsStyle = .short
        return formatter.localizedString(for: date, relativeTo: Date())
    }
    
    var detailedTimestamp: String {
        guard let date = startDate else { return "-" }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.string(from: date)
    }
}

enum BuildResult: String, CaseIterable {
    case success = "SUCCESS"
    case failure = "FAILURE"
    case unstable = "UNSTABLE"
    case aborted = "ABORTED"
    case notBuilt = "NOT_BUILT"
    case building
    case unknown
    
    var color: Color {
        switch self {
        case .success: return .green
        case .failure: return .red
        case .unstable: return .yellow
        case .aborted: return .gray
        case .notBuilt: return .gray
        case .building: return .blue
        case .unknown: return .gray
        }
    }
    
    var iconName: String {
        switch self {
        case .success: return "checkmark.circle.fill"
        case .failure: return "xmark.circle.fill"
        case .unstable: return "exclamationmark.triangle.fill"
        case .aborted: return "stop.circle.fill"
        case .notBuilt: return "circle.dashed"
        case .building: return "circle.dotted"
        case .unknown: return "questionmark.circle.fill"
        }
    }
    
    var displayName: String {
        switch self {
        case .success: return "成功"
        case .failure: return "失败"
        case .unstable: return "不稳定"
        case .aborted: return "已中止"
        case .notBuilt: return "未构建"
        case .building: return "构建中"
        case .unknown: return "未知"
        }
    }
}

// MARK: - API Response Wrappers

struct BuildsResponse: Codable {
    let builds: [Build]
}

struct JobDetailResponse: Codable {
    let name: String
    let url: String
    let color: String?
    let builds: [Build]?
    let lastBuild: BuildReference?
    let lastSuccessfulBuild: BuildReference?
    let lastFailedBuild: BuildReference?
    let description: String?
    let buildable: Bool?
    let property: [JobProperty]?
    
    /// Get parameter definitions from job properties
    var parameterDefinitions: [ParameterDefinition] {
        property?.compactMap { $0.parameterDefinitions }.flatMap { $0 } ?? []
    }
    
    /// Check if this job has parameters
    var hasParameters: Bool {
        !parameterDefinitions.isEmpty
    }
}

// MARK: - Parameter Models

struct JobProperty: Codable {
    let parameterDefinitions: [ParameterDefinition]?
    
    enum CodingKeys: String, CodingKey {
        case parameterDefinitions
    }
}

struct ParameterDefinition: Codable, Identifiable {
    let name: String
    let type: String?
    let description: String?
    let defaultParameterValue: ParameterValue?
    let choices: [String]?
    
    var id: String { name }
    
    /// Parameter type enum for easier handling
    var parameterType: ParameterType {
        guard let type = type else { return .string }
        if type.contains("Choice") || type.contains("ExtendedChoice") {
            return .choice
        } else if type.contains("Boolean") {
            return .boolean
        } else if type.contains("Text") {
            return .text
        } else if type.contains("Password") {
            return .password
        }
        return .string
    }
    
    /// Default value as string
    var defaultValue: String {
        defaultParameterValue?.value ?? ""
    }
}

struct ParameterValue: Codable {
    let name: String?
    let value: String?
    
    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        name = try container.decodeIfPresent(String.self, forKey: .name)
        // value can be String or Bool, try both
        if let stringValue = try? container.decodeIfPresent(String.self, forKey: .value) {
            value = stringValue
        } else if let boolValue = try? container.decodeIfPresent(Bool.self, forKey: .value) {
            value = boolValue ? "true" : "false"
        } else {
            value = nil
        }
    }
    
    enum CodingKeys: String, CodingKey {
        case name, value
    }
}

enum ParameterType: String {
    case string
    case text
    case boolean
    case choice
    case password
}

