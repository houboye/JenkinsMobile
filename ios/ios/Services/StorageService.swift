//
//  StorageService.swift
//  ios
//
//  Local storage service for persisting data
//

import Foundation
import SwiftUI

@MainActor
class StorageService: ObservableObject {
    static let shared = StorageService()
    
    private let serverKey = "jenkins_server"
    private let isLoggedInKey = "is_logged_in"
    
    @Published var server: Server {
        didSet {
            saveServer()
        }
    }
    
    @Published var isLoggedIn: Bool {
        didSet {
            UserDefaults.standard.set(isLoggedIn, forKey: isLoggedInKey)
        }
    }
    
    private init() {
        self.isLoggedIn = UserDefaults.standard.bool(forKey: isLoggedInKey)
        
        if let data = UserDefaults.standard.data(forKey: serverKey),
           let server = try? JSONDecoder().decode(Server.self, from: data) {
            self.server = server
        } else {
            self.server = .empty
        }
    }
    
    private func saveServer() {
        if let data = try? JSONEncoder().encode(server) {
            UserDefaults.standard.set(data, forKey: serverKey)
        }
    }
    
    func login(server: Server) {
        self.server = server
        self.isLoggedIn = true
        JenkinsAPI.shared.configure(with: server)
    }
    
    func logout() {
        self.server = .empty
        self.isLoggedIn = false
    }
    
    func restoreSession() {
        if isLoggedIn && server.isValid {
            JenkinsAPI.shared.configure(with: server)
        }
    }
}

