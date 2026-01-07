//
//  DashboardViewModel.swift
//  ios
//
//  Dashboard screen view model
//

import Foundation
import SwiftUI

@MainActor
class DashboardViewModel: ObservableObject {
    @Published var views: [JenkinsView] = []
    @Published var jobs: [Job] = []
    @Published var selectedViewIndex: Int = 0
    @Published var isLoading = false
    @Published var isRefreshing = false
    @Published var errorMessage: String?
    @Published var showError = false
    @Published var triggerMessage: String?
    @Published var showTriggerAlert = false
    
    private let api = JenkinsAPI.shared
    
    var selectedView: JenkinsView? {
        guard selectedViewIndex < views.count else { return nil }
        return views[selectedViewIndex]
    }
    
    func loadData() async {
        guard !isLoading else { return }
        isLoading = true
        errorMessage = nil
        
        do {
            let fetchedViews = try await api.fetchViews()
            views = fetchedViews
            
            // Load jobs for selected view
            await loadJobsForSelectedView()
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
        await loadJobsForSelectedView()
        isRefreshing = false
    }
    
    func selectView(at index: Int) {
        guard index != selectedViewIndex else { return }
        selectedViewIndex = index
        
        Task {
            await loadJobsForSelectedView()
        }
    }
    
    func loadJobsForSelectedView() async {
        guard let selectedView = selectedView else {
            // Load all jobs if no view selected
            do {
                jobs = try await api.fetchAllJobs()
            } catch {
                // Silently fail, jobs remain empty
            }
            return
        }
        
        do {
            if selectedView.name.lowercased() == "all" {
                jobs = try await api.fetchAllJobs()
            } else {
                jobs = try await api.fetchViewJobs(viewName: selectedView.name)
            }
        } catch let error as APIError {
            errorMessage = error.localizedDescription
            showError = true
        } catch {
            errorMessage = error.localizedDescription
            showError = true
        }
    }
    
    func triggerBuild(for job: Job) {
        Task {
            do {
                try await api.triggerBuild(jobName: job.name)
                triggerMessage = "已触发构建: \(job.name)"
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

