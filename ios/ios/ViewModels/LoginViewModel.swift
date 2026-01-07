//
//  LoginViewModel.swift
//  ios
//
//  Login screen view model
//

import Foundation
import SwiftUI

@MainActor
class LoginViewModel: ObservableObject {
    @Published var serverURL: String = ""
    @Published var username: String = ""
    @Published var apiToken: String = ""
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var showError = false
    
    private let storage = StorageService.shared
    private let api = JenkinsAPI.shared
    
    init() {
        // Load saved server info
        let savedServer = storage.server
        serverURL = savedServer.url
        username = savedServer.username
        apiToken = savedServer.apiToken
    }
    
    var isFormValid: Bool {
        !serverURL.trimmingCharacters(in: .whitespaces).isEmpty &&
        !username.trimmingCharacters(in: .whitespaces).isEmpty &&
        !apiToken.trimmingCharacters(in: .whitespaces).isEmpty
    }
    
    func login() async -> Bool {
        guard isFormValid else {
            errorMessage = "请填写所有字段"
            showError = true
            return false
        }
        
        isLoading = true
        errorMessage = nil
        
        let server = Server(
            url: serverURL.trimmingCharacters(in: .whitespaces),
            username: username.trimmingCharacters(in: .whitespaces),
            apiToken: apiToken.trimmingCharacters(in: .whitespaces)
        )
        
        do {
            let success = try await api.validateConnection(server: server)
            if success {
                storage.login(server: server)
                isLoading = false
                return true
            } else {
                errorMessage = "连接验证失败"
                showError = true
                isLoading = false
                return false
            }
        } catch let error as APIError {
            errorMessage = error.localizedDescription
            showError = true
            isLoading = false
            return false
        } catch {
            errorMessage = "连接失败: \(error.localizedDescription)"
            showError = true
            isLoading = false
            return false
        }
    }
}

