//
//  JobDetailView.swift
//  ios
//
//  Job detail screen with build history
//

import SwiftUI

struct JobDetailView: View {
    let job: Job
    @StateObject private var viewModel: JobDetailViewModel
    
    init(job: Job) {
        self.job = job
        _viewModel = StateObject(wrappedValue: JobDetailViewModel(jobName: job.name, jobURL: job.url))
    }
    
    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // Job Header
                VStack(spacing: 16) {
                    HStack(spacing: 16) {
                        StatusIcon(status: job.status, isBuilding: job.isBuilding, size: 40)
                        
                        VStack(alignment: .leading, spacing: 4) {
                            Text(job.name)
                                .font(.title2)
                                .fontWeight(.bold)
                            
                            if let description = viewModel.jobDetail?.description, !description.isEmpty {
                                Text(description)
                                    .font(.subheadline)
                                    .foregroundColor(.secondary)
                                    .lineLimit(2)
                            }
                        }
                        
                        Spacer()
                    }
                    
                    // Quick Stats
                    HStack(spacing: 20) {
                        StatCard(
                            title: "最新构建",
                            value: job.lastBuild.map { "#\($0.number)" } ?? "-",
                            color: .blue
                        )
                        
                        StatCard(
                            title: "上次成功",
                            value: job.lastSuccessfulBuild.map { "#\($0.number)" } ?? "-",
                            color: .green
                        )
                        
                        StatCard(
                            title: "上次失败",
                            value: job.lastFailedBuild.map { "#\($0.number)" } ?? "-",
                            color: .red
                        )
                    }
                    
                    // Trigger Build Button
                    Button {
                        viewModel.triggerBuild()
                    } label: {
                        HStack {
                            Image(systemName: "play.fill")
                            Text("触发构建")
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                
                Divider()
                
                // Build History
                VStack(alignment: .leading, spacing: 12) {
                    Text("构建历史")
                        .font(.headline)
                        .padding(.horizontal)
                        .padding(.top)
                    
                    if viewModel.isLoading && viewModel.builds.isEmpty {
                        HStack {
                            Spacer()
                            ProgressView()
                            Spacer()
                        }
                        .padding(.vertical, 40)
                    } else if viewModel.builds.isEmpty {
                        HStack {
                            Spacer()
                            VStack(spacing: 8) {
                                Image(systemName: "tray")
                                    .font(.system(size: 32))
                                    .foregroundColor(.secondary)
                                Text("暂无构建记录")
                                    .foregroundColor(.secondary)
                            }
                            Spacer()
                        }
                        .padding(.vertical, 40)
                    } else {
                        LazyVStack(spacing: 0) {
                            ForEach(viewModel.builds) { build in
                                BuildRowView(build: build)
                                Divider()
                                    .padding(.leading, 56)
                            }
                        }
                    }
                }
            }
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("任务详情")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
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
        .refreshable {
            await viewModel.refresh()
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
        .task {
            await viewModel.loadData()
        }
    }
}

struct StatCard: View {
    let title: String
    let value: String
    let color: Color
    
    var body: some View {
        VStack(spacing: 4) {
            Text(value)
                .font(.headline)
                .foregroundColor(color)
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }
}

struct BuildRowView: View {
    let build: Build
    
    var body: some View {
        HStack(spacing: 12) {
            BuildStatusIcon(result: build.buildResult, size: 24)
            
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text("#\(build.number)")
                        .font(.body)
                        .fontWeight(.medium)
                    
                    if let displayName = build.displayName, displayName != "#\(build.number)" {
                        Text(displayName)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                HStack(spacing: 12) {
                    Text(build.buildResult.displayName)
                        .font(.caption)
                        .foregroundColor(build.buildResult.color)
                    
                    Text(build.formattedTimestamp)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Text(build.formattedDuration)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemBackground))
    }
}

#Preview {
    NavigationStack {
        JobDetailView(
            job: Job(
                name: "ios_archive_pipeline",
                url: "https://jenkins.example.com/job/ios_archive_pipeline/",
                color: "blue",
                lastBuild: BuildReference(number: 1318, url: ""),
                lastSuccessfulBuild: BuildReference(number: 1318, url: ""),
                lastFailedBuild: BuildReference(number: 1315, url: ""),
                lastCompletedBuild: nil,
                buildable: true,
                healthReport: [HealthReport(description: "Build stability", score: 80)]
            )
        )
    }
}

