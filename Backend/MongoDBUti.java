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
import java.util.Date;
import java.util.List;

class MongoDBUtil {

    private static final String DATABASE_NAME = "SmartWasteManagement";
    private static final String USER_COLLECTION_NAME = "users";
    private static final String ISSUE_COLLECTION_NAME = "issues";
    private static final String OTP_COLLECTION_NAME = "otps";
    private static final long OTP_EXPIRY_DURATION = 5 * 60 * 1000; // 5 minutes in milliseconds

    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> userCollection;
    private static MongoCollection<Document> issueCollection;
    private static MongoCollection<Document> otpCollection;
    private static GridFSBucket gridFSBucket;

    // Static initialization block to establish MongoDB connection
    static {
        try {
            mongoClient = MongoClients.create("mongodb://localhost:27017");
            database = mongoClient.getDatabase(DATABASE_NAME);
            userCollection = database.getCollection(USER_COLLECTION_NAME);
            issueCollection = database.getCollection(ISSUE_COLLECTION_NAME);
            otpCollection = database.getCollection(OTP_COLLECTION_NAME);
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
     * Finds a user by email. Returns the user document if found, null otherwise.
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
     * Finds a user by driver number. Returns the user document if found, null otherwise.
     */
    public static Document findUserByDriverNumber(String driverNumber) {
        try {
            Document user = userCollection.find(Filters.eq("driverNumber", driverNumber)).first();
            if (user != null) {
                System.out.println("User found with driver number: " + driverNumber);
            } else {
                System.out.println("No user found with driver number: " + driverNumber);
            }
            return user;
        } catch (Exception e) {
            System.err.println("Failed to find user by driver number: " + e.getMessage());
            return null;
        }
    }

    /**
     * Stores an OTP for a given mobile number with timestamp
     */
    public static void storeOTP(String mobile, String otp) {
        try {
            otpCollection.deleteMany(Filters.eq("mobile", mobile));
            Document otpDoc = new Document("mobile", mobile)
                    .append("otp", otp)
                    .append("timestamp", new Date());
            otpCollection.insertOne(otpDoc);
            System.out.println("OTP stored for mobile: " + mobile);
        } catch (Exception e) {
            System.err.println("Failed to store OTP: " + e.getMessage());
            throw new RuntimeException("OTP storage failed", e);
        }
    }

    /**
     * Verifies an OTP for a given mobile number
     * Returns true if OTP is valid and not expired, false otherwise
     */
    public static boolean verifyOTP(String mobile, String otp) {
        try {
            Document otpDoc = otpCollection.find(Filters.eq("mobile", mobile)).first();
            if (otpDoc == null) {
                System.out.println("No OTP found for mobile: " + mobile);
                return false;
            }

            String storedOTP = otpDoc.getString("otp");
            Date timestamp = otpDoc.getDate("timestamp");
            long currentTime = System.currentTimeMillis();
            long otpTime = timestamp.getTime();

            if (currentTime - otpTime > OTP_EXPIRY_DURATION) {
                System.out.println("OTP expired for mobile: " + mobile);
                otpCollection.deleteOne(Filters.eq("mobile", mobile));
                return false;
            }

            boolean isValid = storedOTP.equals(otp);
            if (isValid) {
                System.out.println("OTP verified successfully for mobile: " + mobile);
                otpCollection.deleteOne(Filters.eq("mobile", mobile));
            } else {
                System.out.println("Invalid OTP for mobile: " + mobile);
            }
            return isValid;
        } catch (Exception e) {
            System.err.println("Failed to verify OTP: " + e.getMessage());
            return false;
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
     * Updates an issue with a proof photo and status.
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
     * Updates the password for a user identified by their email.
     */
    public static void updateUserPassword(String email, String newPassword) {
        try {
            Document result = userCollection.findOneAndUpdate(
                    Filters.eq("email", email),
                    Updates.set("password", newPassword)
            );
            if (result != null) {
                System.out.println("Password updated successfully for user: " + email);
            } else {
                System.err.println("No user found with email: " + email + " to update password.");
                throw new RuntimeException("User not found for password update");
            }
        } catch (Exception e) {
            System.err.println("Failed to update user password: " + e.getMessage());
            throw new RuntimeException("Password update failed", e);
        }
    }

    /**
     * Closes the MongoDB client connection and cleans up expired OTPs.
     */
    public static void close() {
        try {
            long currentTime = System.currentTimeMillis();
            otpCollection.deleteMany(
                    Filters.lt("timestamp", new Date(currentTime - OTP_EXPIRY_DURATION))
            );
            System.out.println("Cleaned up expired OTPs.");
        } catch (Exception e) {
            System.err.println("Failed to clean up expired OTPs: " + e.getMessage());
        }

        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            System.out.println("MongoDB connection closed.");
        }
    }

    /**
     * Validates driver login using email and password only.
     * Returns the driver number if authenticated successfully, null otherwise.
     */
    public static String validateDriverLogin(String email, String password) {
        try {
            Document user = userCollection.find(Filters.and(
                    Filters.eq("email", email),
                    Filters.eq("password", password),
                    Filters.eq("role", "Driver")
            )).first();
            if (user != null) {
                String driverNumber = user.getString("driverNumber");
                System.out.println("Driver authenticated: " + email + " with driver number: " + driverNumber);
                return driverNumber;
            } else {
                System.out.println("Driver authentication failed for email: " + email);
                return null;
            }
        } catch (Exception e) {
            System.err.println("Failed to validate driver login: " + e.getMessage());
            return null;
        }
    }
}