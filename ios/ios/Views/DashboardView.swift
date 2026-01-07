//
//  DashboardView.swift
//  ios
//
//  Main dashboard screen with job list
//

import SwiftUI

struct DashboardView: View {
    @StateObject private var viewModel = DashboardViewModel()
    @State private var selectedJob: Job?
    @State private var showSettings = false
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // View Tabs
                if !viewModel.views.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 4) {
                            ForEach(Array(viewModel.views.enumerated()), id: \.element.id) { index, view in
                                ViewTabButton(
                                    title: view.displayName,
                                    isSelected: index == viewModel.selectedViewIndex
                                ) {
                                    viewModel.selectView(at: index)
                                }
                            }
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 8)
                    }
                    .background(Color(.systemBackground))
                    
                    Divider()
                }
                
                // Job List
                if viewModel.isLoading && viewModel.jobs.isEmpty {
                    Spacer()
                    ProgressView("加载中...")
                        .progressViewStyle(CircularProgressViewStyle())
                    Spacer()
                } else if viewModel.jobs.isEmpty {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "tray")
                            .font(.system(size: 48))
                            .foregroundColor(.secondary)
                        Text("没有任务")
                            .font(.headline)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                } else {
                    List {
                        ForEach(viewModel.jobs) { job in
                            NavigationLink(value: job) {
                                JobRowView(job: job) {
                                    viewModel.triggerBuild(for: job)
                                }
                            }
                        }
                    }
                    .listStyle(.plain)
                    .refreshable {
                        await viewModel.refresh()
                    }
                }
            }
            .navigationTitle("Jenkins")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showSettings = true
                    } label: {
                        Image(systemName: "gearshape")
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task {
                            await viewModel.refresh()
                        }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .disabled(viewModel.isRefreshing)
                }
            }
            .navigationDestination(for: Job.self) { job in
                JobDetailView(job: job)
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
            }
            .alert("错误", isPresented: $viewModel.showError) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.errorMessage ?? "未知错误")
            }
            .alert("提示", isPresented: $viewModel.showTriggerAlert) {
                Button("确定", role: .cancel) {}
            } message: {
                Text(viewModel.triggerMessage ?? "")
            }
        }
        .task {
            await viewModel.loadData()
        }
    }
}

struct ViewTabButton: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(
                    isSelected ?
                    Color.blue.opacity(0.15) :
                    Color(.systemGray6)
                )
                .foregroundColor(isSelected ? .blue : .primary)
                .cornerRadius(20)
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    DashboardView()
}

