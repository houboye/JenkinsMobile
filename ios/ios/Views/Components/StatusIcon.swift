//
//  StatusIcon.swift
//  ios
//
//  Status indicator icon component
//

import SwiftUI

struct StatusIcon: View {
    let status: JobStatus
    let isBuilding: Bool
    var size: CGFloat = 24
    
    @State private var isAnimating = false
    
    var body: some View {
        ZStack {
            Image(systemName: status.iconName)
                .font(.system(size: size))
                .foregroundColor(status.color)
                .opacity(isBuilding && isAnimating ? 0.3 : 1.0)
        }
        .onAppear {
            if isBuilding {
                withAnimation(
                    .easeInOut(duration: 0.8)
                    .repeatForever(autoreverses: true)
                ) {
                    isAnimating = true
                }
            }
        }
    }
}

struct BuildStatusIcon: View {
    let result: BuildResult
    var size: CGFloat = 20
    
    @State private var isAnimating = false
    
    var body: some View {
        ZStack {
            Image(systemName: result.iconName)
                .font(.system(size: size))
                .foregroundColor(result.color)
                .opacity(result == .building && isAnimating ? 0.3 : 1.0)
        }
        .onAppear {
            if result == .building {
                withAnimation(
                    .easeInOut(duration: 0.8)
                    .repeatForever(autoreverses: true)
                ) {
                    isAnimating = true
                }
            }
        }
    }
}

#Preview {
    VStack(spacing: 20) {
        HStack(spacing: 20) {
            StatusIcon(status: .success, isBuilding: false)
            StatusIcon(status: .failure, isBuilding: false)
            StatusIcon(status: .unstable, isBuilding: false)
            StatusIcon(status: .disabled, isBuilding: false)
        }
        HStack(spacing: 20) {
            StatusIcon(status: .success, isBuilding: true)
            StatusIcon(status: .failure, isBuilding: true)
        }
        HStack(spacing: 20) {
            BuildStatusIcon(result: .success)
            BuildStatusIcon(result: .failure)
            BuildStatusIcon(result: .building)
        }
    }
    .padding()
}

