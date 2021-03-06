#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint yaband_sunmi_plugins.podspec' to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'yaband_sunmi_plugins'
  s.version          = '0.0.1'
  s.summary          = 'for yaband sunmi plugins'
  s.description      = <<-DESC
for yaband sunmi plugins
                       DESC
  s.homepage         = 'https://github.com/quinnQjc/yaband_sunmi_plugins.git'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'Your Company' => 'email@example.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '8.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
