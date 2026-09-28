import { useEffect } from "react";
import { StatusBar } from "expo-status-bar";
import { NavigationContainer } from "@react-navigation/native";
import { createNativeStackNavigator } from "@react-navigation/native-stack";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { useAuth } from "@/hooks/useAuth";
import { motion } from "framer-motion";
import { Font } from "expo-font";

import { HomeScreen } from "./screens/HomeScreen";
import { LoginScreen } from "./screens/LoginScreen";
import { AdminScreen } from "./screens/AdminScreen";
import { MobileCoursesPage } from "./pages/MobileCoursesPage";
import { MobileLecturesPage } from "./pages/MobileLecturesPage";
import { MobileEnotesPage } from "./pages/MobileEnotesPage";

const Stack = createNativeStackNavigator();

const queryClient = new QueryClient();

function waitForFonts() {
  return Font.loadAsync({
    "sf-pro-display": "SF Pro Display",
    "sf-pro-text": "SF Pro Text",
  });
}

export default function App() {
  const { user, isAdmin, loginAsAdmin, logoutAdmin, trackAppOpen, error } = useAuth();

  useEffect(() => {
    (async () => {
      await waitForFonts();
      trackAppOpen();
    })();
  }, [user]);

  const navigateTo = (page: string) => {
    // Navigate based on page name
    switch (page) {
      case "courses":
        // Navigate to courses screen
        break;
      case "lectures":
        // Navigate to lectures screen
        break;
      case "enotes":
        // Navigate to enotes screen
        break;
    }
  };

  if (!user) {
    return (
      <NavigationContainer>
        <Stack.Navigator initialRouteName="Login">
          <Stack.Screen name="Login" component={LoginScreen} options={{ headerShown: false }} />
          <Stack.Screen name="Home" component={HomeScreen} options={{ title: 'TNC Mobile' }} />
        </Stack.Navigator>
      </NavigationContainer>
    );
  }

  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Home">
        <Stack.Screen name="Home" component={HomeScreen} options={{ title: 'TNC Mobile' }} />
        <Stack.Screen name="Courses" component={MobileCoursesPage} options={{ title: 'Courses' }} />
        <Stack.Screen name="Lectures" component={MobileLecturesPage} options={{ title: 'Lectures' }} />
        <Stack.Screen name="E-Notes" component={MobileEnotesPage} options={{ title: 'E-Notes' }} />
        <Stack.Screen name="Admin" component={AdminScreen} options={{ title: 'Admin Panel' }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}