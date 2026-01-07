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
    
    // 参数对话框状态
    @Published var showParametersSheet = false
    @Published var pendingJob: Job?
    @Published var parameters: [ParameterDefinition] = []
    
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
    
    /// 点击构建按钮时调用 - 先检查是否有参数
    func triggerBuild(for job: Job) {
        Task {
            do {
                // 先获取 job 详情，检查是否有参数
                let jobDetail = try await api.fetchJobDetailByURL(jobURL: job.url)
                let params = jobDetail.parameterDefinitions
                
                if !params.isEmpty {
                    // 有参数，显示参数对话框
                    pendingJob = job
                    parameters = params
                    showParametersSheet = true
                } else {
                    // 无参数，直接触发构建
                    try await api.triggerBuildByURL(jobURL: job.url)
                    triggerMessage = "已触发构建: \(job.name)"
                    showTriggerAlert = true
                    
                    try? await Task.sleep(nanoseconds: 1_000_000_000)
                    await refresh()
                }
            } catch let error as APIError {
                // 获取详情失败，尝试直接构建
                do {
                    try await api.triggerBuildByURL(jobURL: job.url)
                    triggerMessage = "已触发构建: \(job.name)"
                    showTriggerAlert = true
                    
                    try? await Task.sleep(nanoseconds: 1_000_000_000)
                    await refresh()
                } catch {
                    triggerMessage = "触发失败: \(error.localizedDescription)"
                    showTriggerAlert = true
                }
            } catch {
                triggerMessage = "触发失败: \(error.localizedDescription)"
                showTriggerAlert = true
            }
        }
    }
    
    /// 带参数触发构建
    func triggerBuildWithParameters(_ params: [String: String]) {
        guard let job = pendingJob else { return }
        
        Task {
            do {
                try await api.triggerBuildByURL(jobURL: job.url, parameters: params)
                triggerMessage = "已触发构建: \(job.name)"
                showTriggerAlert = true
                showParametersSheet = false
                pendingJob = nil
                parameters = []
                
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
    
    /// 隐藏参数对话框
    func hideParametersSheet() {
        showParametersSheet = false
        pendingJob = nil
        parameters = []
    }
}
