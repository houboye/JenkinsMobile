//
//  JobDetailViewModel.swift
//  ios
//
//  Job detail screen view model
//

import Foundation
import SwiftUI

@MainActor
class JobDetailViewModel: ObservableObject {
    @Published var jobDetail: JobDetailResponse?
    @Published var builds: [Build] = []
    @Published var isLoading = false
    @Published var isRefreshing = false
    @Published var errorMessage: String?
    @Published var showError = false
    @Published var triggerMessage: String?
    @Published var showTriggerAlert = false
    @Published var showParametersSheet = false
    
    private let api = JenkinsAPI.shared
    let jobName: String
    let jobURL: String
    
    init(jobName: String, jobURL: String) {
        self.jobName = jobName
        self.jobURL = jobURL
    }
    
    /// Check if the job has parameters
    var hasParameters: Bool {
        jobDetail?.hasParameters ?? false
    }
    
    /// Get parameter definitions
    var parameters: [ParameterDefinition] {
        jobDetail?.parameterDefinitions ?? []
    }
    
    func loadData() async {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil
        
        do {
            let detail = try await api.fetchJobDetailByURL(jobURL: jobURL)
            jobDetail = detail
            builds = detail.builds ?? []
        } catch let error as APIError {
            errorMessage = error.localizedDescription
            showError = true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    func refresh() async {
        isRefreshing = true
        
        do {
            let detail = try await api.fetchJobDetailByURL(jobURL: jobURL)
            jobDetail = detail
            builds = detail.builds ?? []
        } catch {
            // Silently fail on refresh
        }
        
        isRefreshing = false
    }
    
    /// Called when user taps the trigger build button
    func onTriggerBuildTapped() {
        if hasParameters {
            // Show parameters sheet
            showParametersSheet = true
        } else {
            // No parameters, trigger directly
            triggerBuild(with: nil)
        }
    }
    
    /// Trigger build with optional parameters
    func triggerBuild(with parameters: [String: String]?) {
        Task {
            do {
                try await api.triggerBuildByURL(jobURL: jobURL, parameters: parameters)
                triggerMessage = "已触发构建"
                showTriggerAlert = true
                
                // Refresh after a short delay
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                await refresh()
            } catch let error as APIError {
                triggerMessage = "触发失败: \(error.localizedDescription)"
                showTriggerAlert = true
            } catch {
                triggerMessage = "触发失败: \(error.localizedDescription)"
                showTriggerAlert = true
            }
        }
    }
}

