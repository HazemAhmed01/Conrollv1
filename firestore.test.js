const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const PROJECT_ID = "demo-no-project";
const DATABASE_ID = "ai-studio-android-tubemark-a61e1033-c391-4501-aa2e-fbf7e3f9229e";

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write anything", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc("userA").collection("folders").get());
  await assertFails(unauthDb.collection("users").doc("userA").collection("folders").doc("folder1").set({
    userId: "userA",
    name: "Study",
    colorHex: "#E53935",
    createdAt: new Date()
  }));
});

test("Authenticated user can create, read, update, and delete their own folder", async () => {
  const userADb = testEnv.authenticatedContext("userA").firestore();
  const folderRef = userADb.collection("users").doc("userA").collection("folders").doc("folder1");

  // Create
  await assertSucceeds(folderRef.set({
    userId: "userA",
    name: "Study",
    colorHex: "#E53935",
    iconName: "folder",
    createdAt: new Date()
  }));

  // Read
  await assertSucceeds(folderRef.get());

  // Update
  await assertSucceeds(folderRef.update({
    name: "Study & Work",
    colorHex: "#1E88E5",
    updatedAt: new Date()
  }));

  // Delete
  await assertSucceeds(folderRef.delete());
});

test("User cannot read or write another user's folders or videos", async () => {
  // Setup userA's data with admin context
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const adminDb = context.firestore();
    await adminDb.collection("users").doc("userA").collection("folders").doc("folder1").set({
      userId: "userA",
      name: "Secret Folder",
      colorHex: "#E53935",
      createdAt: new Date()
    });
  });

  const userBDb = testEnv.authenticatedContext("userB").firestore();
  const targetFolder = userBDb.collection("users").doc("userA").collection("folders").doc("folder1");

  // User B tries to read User A's folder
  await assertFails(targetFolder.get());
  // User B tries to write to User A's folder
  await assertFails(targetFolder.set({
    userId: "userA",
    name: "Hacked",
    colorHex: "#E53935",
    createdAt: new Date()
  }));
});

test("Authenticated user can create, read, and update their own video bookmark", async () => {
  const userADb = testEnv.authenticatedContext("userA").firestore();
  const videoRef = userADb.collection("users").doc("userA").collection("videos").doc("video1");

  await assertSucceeds(videoRef.set({
    userId: "userA",
    youtubeVideoId: "dQw4w9WgXcQ",
    originalUrl: "https://youtu.be/dQw4w9WgXcQ?t=45",
    title: "Rick Astley",
    channelName: "Rick Astley",
    thumbnailUrl: "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
    startSeconds: 45,
    folderId: "folder1",
    note: "Great chorus",
    isFavorite: true,
    createdAt: new Date()
  }));

  await assertSucceeds(videoRef.get());

  // Update startSeconds and note
  await assertSucceeds(videoRef.update({
    startSeconds: 60,
    note: "Updated note",
    updatedAt: new Date()
  }));
});
