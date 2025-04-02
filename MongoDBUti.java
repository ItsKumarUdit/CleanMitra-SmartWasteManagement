package com.example.javafx;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

class MongoDBUtil {

    private static final String DATABASE_NAME = "SmartWasteManagement";
    private static final String USER_COLLECTION_NAME = "users";
    private static final String ISSUE_COLLECTION_NAME = "issues";

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> userCollection;
    private static MongoCollection<Document> issueCollection;
    private static GridFSBucket gridFSBucket;

    // Static initialization block to establish MongoDB connection
    static {
        try {
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            database = mongoClient.getDatabase(DATABASE_NAME);
            userCollection = database.getCollection(USER_COLLECTION_NAME);
            issueCollection = database.getCollection(ISSUE_COLLECTION_NAME);
            gridFSBucket = GridFSBuckets.create(database);
            System.out.println("Connected to MongoDB successfully!");
        } catch (Exception e) {
            System.err.println("Failed to connect to MongoDB: " + e.getMessage());
            throw new RuntimeException("MongoDB initialization failed", e);
        }
    }

    /**
     * Inserts a new user into the users collection with email, password, role, and optional driverNumber.
     */
    public static void insertUser(String email, String password, String role, String driverNumber) {
        try {
            Document user = new Document("email", email)
                    .append("password", password)
                    .append("role", role);
            if (driverNumber != null && role.equals("Driver")) {
                user.append("driverNumber", driverNumber);
            }
            userCollection.insertOne(user);
            System.out.println("User inserted: " + email + " with role: " + role +
                    (driverNumber != null ? " and driverNumber: " + driverNumber : ""));
        } catch (Exception e) {
            System.err.println("Failed to insert user: " + e.getMessage());
            throw new RuntimeException("User insertion failed", e);
        }
    }

    /**
     * Overloaded method for non-driver users
     */
    public static void insertUser(String email, String password, String role) {
        insertUser(email, password, role, null);
    }

    /**
     * Finds a user by email in the users collection.
     * Returns the user document if found, null otherwise.
     */
    public static Document findUser(String email) {
        try {
            Document user = userCollection.find(Filters.eq("email", email)).first();
            if (user != null) {
                System.out.println("User found: " + email);
            } else {
                System.out.println("No user found with email: " + email);
            }
            return user;
        } catch (Exception e) {
            System.err.println("Failed to find user: " + e.getMessage());
            return null;
        }
    }

    /**
     * Inserts a new issue into the issues collection, optionally with a photo.
     * Returns the ObjectId of the inserted issue.
     */
    public static ObjectId insertIssue(Document issue, File photo) {
        try {
            ObjectId photoId = null;
            if (photo != null) {
                try (FileInputStream fis = new FileInputStream(photo)) {
                    photoId = gridFSBucket.uploadFromStream(photo.getName(), fis);
                    issue.append("photoId", photoId);
                }
            }
            issueCollection.insertOne(issue);
            ObjectId issueId = issue.getObjectId("_id");
            System.out.println("Issue inserted with ID: " + issueId);
            return issueId;
        } catch (Exception e) {
            System.err.println("Failed to insert issue: " + e.getMessage());
            return null;
        }
    }

    /**
     * Retrieves a list of all pending issues from the issues collection.
     */
    public static List<Document> getPendingIssues() {
        try {
            List<Document> pendingIssues = issueCollection
                    .find(Filters.eq("status", "Pending"))
                    .into(new ArrayList<>());
            System.out.println("Fetched " + pendingIssues.size() + " pending issues.");
            return pendingIssues;
        } catch (Exception e) {
            System.err.println("Failed to fetch pending issues: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Finds an issue by its ObjectId in the issues collection.
     * Returns the issue document if found, null otherwise.
     */
    public static Document findIssueById(ObjectId issueId) {
        try {
            Document issue = issueCollection.find(Filters.eq("_id", issueId)).first();
            if (issue != null) {
                System.out.println("Issue found with ID: " + issueId);
            } else {
                System.out.println("No issue found with ID: " + issueId);
            }
            return issue;
        } catch (Exception e) {
            System.err.println("Failed to find issue by ID: " + e.getMessage());
            return null;
        }
    }

    /**
     * Updates the status of an issue identified by its ObjectId.
     */
    public static void updateIssueStatus(ObjectId issueId, String status) {
        try {
            issueCollection.updateOne(
                    Filters.eq("_id", issueId),
                    Updates.set("status", status)
            );
            System.out.println("Issue status updated to: " + status + " for ID: " + issueId);
        } catch (Exception e) {
            System.err.println("Failed to update issue status: " + e.getMessage());
            throw new RuntimeException("Issue status update failed", e);
        }
    }

    /**
     * Updates an issue with a new status and optionally a proof photo.
     */
    public static void updateIssueWithProof(ObjectId issueId, String status, File proofPhoto) {
        try {
            ObjectId proofPhotoId = null;
            if (proofPhoto != null) {
                try (FileInputStream fis = new FileInputStream(proofPhoto)) {
                    proofPhotoId = gridFSBucket.uploadFromStream(proofPhoto.getName(), fis);
                }
            }
            issueCollection.updateOne(
                    Filters.eq("_id", issueId),
                    Updates.combine(
                            Updates.set("status", status),
                            Updates.set("ProofPhotoID", proofPhotoId)
                    )
            );
            System.out.println("Issue updated with status: " + status + " and ProofPhotoID: " + proofPhotoId + " for ID: " + issueId);
        } catch (Exception e) {
            System.err.println("Failed to update issue with proof: " + e.getMessage());
            throw new RuntimeException("Issue update with proof failed", e);
        }
    }

    /**
     * Deletes all issues and their associated photos from the database.
     */
    public static void clearIssues() {
        try {
            for (Document issue : issueCollection.find()) {
                ObjectId photoId = issue.getObjectId("photoId");
                if (photoId != null) {
                    gridFSBucket.delete(photoId);
                    System.out.println("Deleted photo with ID: " + photoId);
                }
                ObjectId proofPhotoId = issue.getObjectId("ProofPhotoID");
                if (proofPhotoId != null) {
                    gridFSBucket.delete(proofPhotoId);
                    System.out.println("Deleted proof photo with ID: " + proofPhotoId);
                }
            }
            issueCollection.deleteMany(new Document());
            System.out.println("All issues and associated photos cleared from the database.");
        } catch (Exception e) {
            System.err.println("Failed to clear issues: " + e.getMessage());
            throw new RuntimeException("Failed to clear issues", e);
        }
    }

    /**
     * Retrieves the photo stream for a given ObjectId from GridFS.
     * Returns null if the photoId is invalid or retrieval fails.
     */
    public static InputStream getPhotoStream(ObjectId photoId) {
        try {
            if (photoId != null) {
                InputStream stream = gridFSBucket.openDownloadStream(photoId);
                System.out.println("Retrieved photo stream for ID: " + photoId);
                return stream;
            }
            System.out.println("No photo ID provided for retrieval.");
            return null;
        } catch (Exception e) {
            System.err.println("Failed to retrieve photo: " + e.getMessage());
            return null;
        }
    }

    /**
     * Closes the MongoDB client connection.
     */
    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            System.out.println("MongoDB connection closed.");
        }
    }
}