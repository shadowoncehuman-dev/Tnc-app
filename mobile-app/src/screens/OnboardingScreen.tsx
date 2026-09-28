import React, { useEffect } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, Platform } from 'react-native';
import { useAuth } from '../hooks/useAuth';

export const OnboardingScreen = ({ navigation }: any) => {
  const [name, setName] = useState('');
  const { user, isLoading, saveUserInfo, trackAppOpen } = useAuth();

  useEffect(() => {
    if (!isLoading && user) {
      navigation.replace('Home');
    }
  }, [user, isLoading]);

  const handleContinue = async () => {
    if (!name.trim()) {
      Alert.alert('Error', 'Please enter your name');
      return;
    }

    // Save user info with device, IP, network
    const deviceId = await AsyncStorageGetItemAsync('device_id') || generateDeviceId();
    const ipAddress = await AsyncStorageGetItemAsync('ip_address') || '0.0.0.0';
    const networkInfo = await AsyncStorageGetItemAsync('network_info') || 'unknown';

    await saveUserInfo(name, deviceId, ipAddress, networkInfo);
    trackAppOpen();
    navigation.replace('Home');
  };

  if (isLoading) {
    return null;
  }

  return (
    <View style={styles.container}>
      {user ? null : (
        <>
          <Text style={styles.title}>Welcome to TNC</Text>
          <Text style={styles.subtitle}>Please enter your name</Text>
          <TextInput
            style={styles.input}
            placeholder="Your Name"
            value={name}
            onChangeText={setName}
            autoCapitalize="words"
          />
          <TouchableOpacity style={styles.button} onPress={handleContinue}>
            <Text style={styles.buttonText}>Continue</Text>
          </TouchableOpacity>
        </>
      )}
      {user && (
        <View style={styles.greetingContainer}>
          <Text style={styles.greeting}>Hello, {user.name}!</Text>
          <Text style={styles.subText}>Welcome back to TNC</Text>
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#f5f5f5',
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    marginBottom: 10,
    color: '#333',
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
    marginBottom: 25,
  },
  input: {
    width: '100%',
    height: 50,
    backgroundColor: 'white',
    borderRadius: 8,
    paddingHorizontal: 15,
    marginBottom: 15,
    borderWidth: 1,
    borderColor: '#ddd',
    fontSize: 16,
  },
  button: {
    width: '100%',
    height: 50,
    backgroundColor: '#007AFF',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 10,
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
  greetingContainer: {
    padding: 30,
    borderRadius: 10,
    backgroundColor: 'white',
    shadowColor: '#000',
    shadowOpacity: 0.1,
    shadowRadius: 10,
    elevation: 5,
  },
  greeting: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 5,
  },
  subText: {
    fontSize: 14,
    color: '#666',
  },
});