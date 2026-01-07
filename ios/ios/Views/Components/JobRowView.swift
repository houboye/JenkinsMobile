//
//  JobRowView.swift
//  ios
//
//  Job list row component
//

import SwiftUI

struct JobRowView: View {
    let job: Job
    let onTriggerBuild: () -> Void
    
    @State private var isTriggeringBuild = false
    
    var body: some View {
        HStack(spacing: 12) {
            // Status Icon
            StatusIcon(status: job.status, isBuilding: job.isBuilding, size: 20)
            
            // Weather Icon
            WeatherIcon(healthScore: job.healthScore, size: 18)
            
            // Job Info
            VStack(alignment: .leading, spacing: 4) {
                Text(job.name)
                    .font(.body)
                    .fontWeight(.medium)
                    .foregroundColor(.primary)
                    .lineLimit(1)
                
                HStack(spacing: 12) {
                    if let lastBuild = job.lastBuild {
                        Label("#\(lastBuild.number)", systemImage: "number")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    if let lastSuccessful = job.lastSuccessfulBuild {
                        Label("成功 #\(lastSuccessful.number)", systemImage: "checkmark")
                            .font(.caption)
                            .foregroundColor(.green)
                    }
                    
                    if let lastFailed = job.lastFailedBuild {
                        Label("失败 #\(lastFailed.number)", systemImage: "xmark")
                            .font(.caption)
                            .foregroundColor(.red)
                    }
                }
            }
            
            Spacer()
            
            // Trigger Build Button
            Button {
                triggerBuild()
            } label: {
                if isTriggeringBuild {
                    ProgressView()
                        .progressViewStyle(CircularProgressViewStyle())
                        .frame(width: 32, height: 32)
                } else {
                    Image(systemName: "play.circle.fill")
                        .font(.title2)
                        .foregroundColor(.blue)
                }
            }
            .buttonStyle(.plain)
            .disabled(isTriggeringBuild || !(job.buildable ?? true))
        }
        .padding(.vertical, 8)
        .contentShape(Rectangle())
    }
    
    private func triggerBuild() {
        isTriggeringBuild = true
        onTriggerBuild()
        
        // Reset after a delay
        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
            isTriggeringBuild = false
        }
    }
}

#Preview {
    List {
        JobRowView(
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
            ),
            onTriggerBuild: {}
        )
        
        JobRowView(
            job: Job(
                name: "flutter_sync_event_iOS",
                url: "https://jenkins.example.com/job/flutter_sync_event_iOS/",
                color: "red",
                lastBuild: BuildReference(number: 2, url: ""),
                lastSuccessfulBuild: nil,
                lastFailedBuild: BuildReference(number: 2, url: ""),
                lastCompletedBuild: nil,
                buildable: true,
                healthReport: [HealthReport(description: "Build stability", score: 20)]
            ),
            onTriggerBuild: {}
        )
        
        JobRowView(
            job: Job(
                name: "ios_change_build_num",
                url: "https://jenkins.example.com/job/ios_change_build_num/",
                color: "blue_anime",
                lastBuild: BuildReference(number: 623, url: ""),
                lastSuccessfulBuild: BuildReference(number: 623, url: ""),
                lastFailedBuild: BuildReference(number: 358, url: ""),
                lastCompletedBuild: nil,
                buildable: true,
                healthReport: [HealthReport(description: "Build stability", score: 100)]
            ),
            onTriggerBuild: {}
        )
    }
}

