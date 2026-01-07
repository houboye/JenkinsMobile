//
//  Server.swift
//  ios
//
//  Jenkins server configuration model
//

import Foundation

struct Server: Codable, Equatable {
    var url: String
    var username: String
    var apiToken: String
    
    var isValid: Bool {
        !url.isEmpty && !username.isEmpty && !apiToken.isEmpty
    }
    
    var baseURL: URL? {
        var urlString = url.trimmingCharacters(in: .whitespacesAndNewlines)
        if !urlString.hasPrefix("http://") && !urlString.hasPrefix("https://") {
            urlString = "https://" + urlString
        }
        if urlString.hasSuffix("/") {
            urlString = String(urlString.dropLast())
        }
        return URL(string: urlString)
    }
    
    var authHeader: String {
        let credentials = "\(username):\(apiToken)"
        let data = credentials.data(using: .utf8)!
        return "Basic \(data.base64EncodedString())"
    }
    
    static let empty = Server(url: "", username: "", apiToken: "")
}

