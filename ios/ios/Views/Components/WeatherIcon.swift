//
//  WeatherIcon.swift
//  ios
//
//  Weather indicator icon based on health score
//

import SwiftUI

struct WeatherIcon: View {
    let healthScore: Int
    var size: CGFloat = 24
    
    var body: some View {
        Image(systemName: iconName)
            .font(.system(size: size))
            .foregroundColor(iconColor)
    }
    
    private var iconName: String {
        switch healthScore {
        case 81...100:
            return "sun.max.fill"
        case 61...80:
            return "cloud.sun.fill"
        case 41...60:
            return "cloud.fill"
        case 21...40:
            return "cloud.rain.fill"
        default:
            return "cloud.bolt.rain.fill"
        }
    }
    
    private var iconColor: Color {
        switch healthScore {
        case 81...100:
            return .yellow
        case 61...80:
            return .orange
        case 41...60:
            return Color(.systemGray)
        case 21...40:
            return Color(.systemGray2)
        default:
            return Color(.systemGray3)
        }
    }
}

#Preview {
    HStack(spacing: 20) {
        WeatherIcon(healthScore: 100)
        WeatherIcon(healthScore: 80)
        WeatherIcon(healthScore: 60)
        WeatherIcon(healthScore: 40)
        WeatherIcon(healthScore: 20)
    }
    .padding()
}

