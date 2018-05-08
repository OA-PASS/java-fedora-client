/*
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dataconservancy.pass.client.integration;

import java.net.URI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import org.dataconservancy.pass.model.Deposit;
import org.dataconservancy.pass.model.Deposit.DepositStatus;
import org.dataconservancy.pass.model.Grant;
import org.dataconservancy.pass.model.Grant.AwardStatus;
import org.dataconservancy.pass.model.PassEntity;
import org.dataconservancy.pass.model.Submission;
import org.dataconservancy.pass.model.Submission.AggregatedDepositStatus;
import org.dataconservancy.pass.model.User;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for PassClient.findAllByAttributes
 * @author Karen Hanson
 */
public class FindAllByAttributesIT extends ClientITBase {
    
    /**
     * Ensures that index search can handle special characters including umlauts, double quotes, 
     * dashes, and ampersands
     */
    @Test
    public void testSpecialCharacterSearch() {
        User user = new User();
        user.setFirstName("Mary-Ann");
        user.setLastName("Schäfer");
        user.setDisplayName("Mary \"The Shark\" Schäfer");
        user.setAffiliation("Lamar & Schäfer Laboratory, Nürnberg");
        URI userId1 = client.createResource(user);
        URI userId2 = client.createResource(user);
        
        //thow in another record that should not match
        URI userId3 = client.createResource(random(User.class, 1));
        
        try {
            //make sure records are in the indexer before continuing
            attempt(RETRIES, () -> {
                final URI uri = client.findByAttribute(User.class, "@id", userId1);
                assertEquals(userId1, uri);
            });        
            attempt(RETRIES, () -> {
                final URI uri = client.findByAttribute(User.class, "@id", userId2);
                assertEquals(userId2, uri);
            });           
            attempt(RETRIES, () -> {
                final URI uri = client.findByAttribute(User.class, "@id", userId3);
                assertEquals(userId3, uri);
            });    
            
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("firstName", user.getFirstName());
            map.put("lastName", user.getLastName());
            map.put("displayName", user.getDisplayName());
            map.put("affiliation", user.getAffiliation());
                        
            Set<URI> uris = client.findAllByAttributes(User.class, map);
            assertEquals(2, uris.size());
            assertTrue(uris.contains(userId1));
            assertTrue(uris.contains(userId2));
            
        } finally {
            //need to log fail if this doesn't work as it could mess up re-testing if data isn't cleaned out
            try {
                if (userId1 != null) {
                    client.deleteResource(userId1);
                }
                if (userId2 != null) {
                    client.deleteResource(userId2);
                }
                if (userId3 != null) {
                    client.deleteResource(userId3);
                }
            } catch (Exception ex) {
                fail("Could not clean up from FindAllByAttributeIT.testSpecialCharacterSearch(), this may cause errors if test is rerun on the same dbs");
            }
        }
        
    }

    @Test
    public void testFindSubmissionWithNoSource() throws Exception {
        URI user = URI.create("http://example.com/user");
        Submission submission1 = random(Submission.class, 1);
        submission1.setSource(null);
        submission1.setMetadata("foo");
        submission1.setUser(user);
        URI expectedUri1 = client.createResource(submission1);

        Submission submission2 = random(Submission.class, 1);
        submission2.setSource(null);
        submission2.setMetadata("foo");
        submission2.setUser(user);
        URI expectedUri2 = client.createResource(submission2);

        Submission submission3 = random(Submission.class, 1);
        submission2.setSource(Submission.Source.OTHER);
        submission2.setMetadata("foo");
        submission2.setUser(user);
        URI expectedUri3 = client.createResource(submission3);

        Deposit deposit1 = random(Deposit.class, 1);
        URI expectedUri4 = client.createResource(deposit1);

        try {
            attempt(30, () -> {
                assertEquals(expectedUri1, client.findByAttribute(Submission.class, "@id", expectedUri1));
                assertEquals(expectedUri2, client.findByAttribute(Submission.class, "@id", expectedUri2));
                assertEquals(expectedUri3, client.findByAttribute(Submission.class, "@id", expectedUri3));
                assertEquals(expectedUri4, client.findByAttribute(Deposit.class, "@id", expectedUri4));
            });

            Set<URI> uris = client.findAllByAttributes(Submission.class, new HashMap<String, Object>() {{
                put("metadata", "foo");
                put("source", null);
                put("user", user);
            }});

            // Only the two Submissions with null sources should be found.  The other Submission has a non-null source,
            // and the other resource is a Deposit.
            assertEquals(2, uris.size());
            assertTrue(uris.stream().anyMatch(uri -> uri.getPath().equals(expectedUri1.getPath())));
            assertTrue(uris.stream().anyMatch(uri -> uri.getPath().equals(expectedUri2.getPath())));

            uris = client.findAllByAttributes(Submission.class, new HashMap<String, Object>() {{
                put("source", null);
            }});

            // Only two Submissions with null sources should be found. The other Submission has a non-null source, and
            // the other resource is a Deposit.
            assertEquals(2, uris.size());
            assertTrue(uris.stream().anyMatch(uri -> uri.getPath().equals(expectedUri1.getPath())));
            assertTrue(uris.stream().anyMatch(uri -> uri.getPath().equals(expectedUri2.getPath())));
        } finally {
            client.deleteResource(expectedUri1);
            client.deleteResource(expectedUri2);
            client.deleteResource(expectedUri3);
            client.deleteResource(expectedUri4);
        }
    }
    
    /**
     * Adds 10 records, then retrieves them in chunks using limit and offet to verify they are working
     * @throws Exception
     */
    @Test
    public void testLimitAndOffset() throws Exception {
        URI repoUri = new URI("fake:repo");
        
        Map<String, Object> attribs = new HashMap<String, Object>();
        attribs.put("depositStatus", DepositStatus.ACCEPTED);
        attribs.put("repository", repoUri);
        
        URI uri = null;
        try {
            for(int i = 0; i < 10; i++){
                Deposit deposit = random(Deposit.class, 2);
                deposit.setDepositStatus(DepositStatus.ACCEPTED);
                deposit.setRepository(repoUri);
                uri = client.createResource(deposit);
            }
            
            final URI searchUri = uri;
            
            attempt(RETRIES, () -> { //make sure last one is in the index
                final URI matchedUri = client.findByAttribute(Deposit.class, "@id", searchUri);
                assertEquals(searchUri, matchedUri);
            }); 
            Set<URI> matches = client.findAllByAttributes(Deposit.class, attribs, 4, 0);
            assertEquals(4, matches.size());
            matches = client.findAllByAttributes(Deposit.class, attribs, 4, 4);
            assertEquals(4, matches.size());
            matches = client.findAllByAttributes(Deposit.class, attribs, 4, 8);
            assertEquals(2, matches.size());
        } finally {
            Set <URI> matches = client.findAllByAttributes(Deposit.class, attribs, 20, 0);
            for (URI match : matches) {
                client.deleteResource(match);
            }
        }
    }
    
    
    
    /**
     * Ensures no match found returns empty Set instead of exception
     */
    @Test
    public void testNoMatchFound() {
        Grant grant = random(Grant.class, 1);
        URI grantId = client.createResource(grant); //create something so it's not empty index
        
        try {
            attempt(RETRIES, () -> {
                URI uri = client.findByAttribute(Grant.class, "@id", grantId);
                assertEquals(grantId, uri);
            });        
                    
            Map<String,Object> flds = new HashMap<String,Object>();
            flds.put("awardNumber", "no match");
            flds.put("awardStatus", AwardStatus.PRE_AWARD.toString());
            Set<URI> matchedIds = client.findAllByAttributes(Grant.class, flds);
            assertNotNull(matchedIds);
            assertEquals(0, matchedIds.size());

        } finally {
            client.deleteResource(grantId);
        }
            
    }

    
    /**
     * Check findAllByAttributes doesn't accept PassEntity as a class param
     */
    @Test(expected=IllegalArgumentException.class)
    public void testBadClassParam() {
        Map<String,Object> map = new HashMap<String,Object>();
        map.put("fake", "fake");
        try {
            client.findAllByAttributes(PassEntity.class, map);
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("modelClass cannot be the abstract"));
            assertTrue(ex instanceof IllegalArgumentException);
            throw ex;
        }
        fail ("Test should have thrown exception");        
    }

    /**
     * Check findAllByAttributes reject null modelClass as a param
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNullClassParam() {
        try {
            Map<String,Object> map = new HashMap<String,Object>();
            map.put("fake", "fake");
            client.findAllByAttributes(null, map);
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("modelClass cannot be null"));
            assertTrue(ex instanceof IllegalArgumentException);
            throw ex;
        }
        fail ("Test should have thrown exception");
    }

    /**
     * Check findAllByAttributes rejects empty map
     */
    @Test(expected=IllegalArgumentException.class)
    public void testEmptyMapClassParam() {
        try {
            Map<String,Object> map = new HashMap<String,Object>();
            client.findAllByAttributes(Deposit.class, map);
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("cannot be empty"));
            assertTrue(ex instanceof IllegalArgumentException);
            throw ex;
        }
        fail ("Test should have thrown exception");
    }

    /**
     * Check findAllByAttributes rejects null map
     */
    @Test(expected=IllegalArgumentException.class)
    public void testNullMapClassParam() {
        try {
            client.findAllByAttributes(Deposit.class, null);
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("cannot be empty"));
            assertTrue(ex instanceof IllegalArgumentException);
            throw ex;
        }
        fail ("Test should have thrown exception");
    }

    /**
     * Check findAllByAttributes rejects a value that is a collection
     */
    @Test(expected=IllegalArgumentException.class)
    public void testValueParamAsCollection() {
        try {
            Map<String,Object> map = new HashMap<String,Object>();
            List<URI> coll = new ArrayList<URI>(); 
            map.put("repositories", coll);
            map.put("AggregatedDepositStatus", AggregatedDepositStatus.ACCEPTED);
            client.findAllByAttributes(Submission.class, map);
        } catch (Exception ex) {
            assertTrue(ex.getMessage().contains("cannot be a Collection"));
            assertTrue(ex instanceof RuntimeException);
            throw ex;
        }
        fail ("Test should have thrown exception");
    }
    
    
    
}
