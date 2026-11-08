/* global describe, beforeEach, cy, it */

// UC-STU-deactivate-student: cancellation sends no lifecycle request; confirmation sends one.

const student = {
  id: 4,
  username: 'j.smith@abc.edu',
  firstName: 'John',
  lastName: 'Smith',
  email: 'j.smith@abc.edu',
  enabled: true,
  roles: 'student',
  sectionId: 2,
  sectionName: 'Section2',
  teamId: 1,
  teamName: 'Team1'
}

const studentResponse = (data) => ({
  flag: true,
  code: 200,
  message: 'Find student successfully',
  data
})

const searchResponse = (data) => ({
  flag: true,
  code: 200,
  message: 'Find students successfully',
  data: {
    content: [data],
    number: 0,
    size: 50,
    totalElements: 1
  }
})

describe('Student deactivation', () => {
  beforeEach(() => {
    cy.intercept('POST', '**/users/login', {
      body: {
        flag: true,
        code: 200,
        message: 'Login successfully',
        data: {
          token: 'eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJleHAiOjQxMDI0NDgwMDB9.test',
          userInfo: { roles: 'admin instructor', defaultSectionId: 2, defaultCourseId: 1 }
        }
      }
    })
    cy.login('b.gates@abc.edu', '123456')
    cy.intercept('GET', '**/students/4', { body: studentResponse(student) }).as('studentDetails')
  })

  it('explains consequences and sends no deactivation request when canceled (extension 4a)', () => {
    cy.intercept('POST', '**/students/search*', { body: searchResponse(student) }).as(
      'studentSearch'
    )
    cy.intercept('PATCH', '**/students/4/deactivation').as('deactivation')
    cy.visit('/students')
    cy.wait('@studentSearch')

    cy.get('[data-cy=student-deactivate]').click()
    cy.wait('@studentDetails')

    cy.get('.el-message-box')
      .should('contain.text', 'prevent them from authenticating')
      .and('contain.text', 'protected resources')
      .and('contain.text', 'weekly activity reports or peer evaluations')
      .and('contain.text', 'team assignment')
      .and('contain.text', 'previously submitted work')
      .and('contain.text', 'This is not deletion')
      .and('contain.text', 'reactivated in the future')
      .contains('button', 'Cancel')
      .click()

    cy.get('@deactivation.all').should('have.length', 0)
    cy.get('[data-cy=student-enabled]').should('be.visible')
  })

  it('deactivates the student after confirmation and refreshes the status', () => {
    let enabled = true
    cy.intercept('POST', '**/students/search*', (request) => {
      request.reply({ body: searchResponse({ ...student, enabled }) })
    }).as('studentSearch')
    cy.intercept('PATCH', '**/students/4/deactivation', (request) => {
      enabled = false
      request.reply({ body: studentResponse({ ...student, enabled: false }) })
    }).as('deactivation')
    cy.visit('/students')
    cy.wait('@studentSearch')

    cy.get('[data-cy=student-deactivate]').click()
    cy.wait('@studentDetails')
    cy.get('.el-message-box').contains('button', 'Deactivate').click()

    cy.wait('@deactivation').its('request.method').should('eq', 'PATCH')
    cy.wait('@studentSearch')
    cy.get('[data-cy=student-disabled]').should('be.visible')
    cy.get('[data-cy=student-deactivate]').should('not.exist')
  })

  it('reports deactivation success when refreshing the student list fails', () => {
    let searchRequests = 0
    cy.intercept('POST', '**/students/search*', (request) => {
      searchRequests += 1
      if (searchRequests === 1) {
        request.reply({ body: searchResponse(student) })
      } else {
        request.reply({
          statusCode: 500,
          body: { flag: false, code: 500, message: 'Unable to refresh students' }
        })
      }
    }).as('studentSearch')
    cy.intercept('PATCH', '**/students/4/deactivation', {
      body: studentResponse({ ...student, enabled: false })
    }).as('deactivation')
    cy.visit('/students')
    cy.wait('@studentSearch')

    cy.get('[data-cy=student-deactivate]').click()
    cy.wait('@studentDetails')
    cy.get('.el-message-box').contains('button', 'Deactivate').click()

    cy.wait('@deactivation')
    cy.wait('@studentSearch').its('response.statusCode').should('eq', 500)
    cy.get('.el-message--warning').should(
      'contain.text',
      'John Smith was deactivated, but the student list could not be refreshed.'
    )
    cy.get('[data-cy=student-disabled]').should('be.visible')
    cy.get('[data-cy=student-deactivate]').should('not.exist')
    cy.get('.el-loading-mask').should('not.exist')
  })

  it('sends only one deactivation request after rapid activation', () => {
    let enabled = true
    cy.intercept('POST', '**/students/search*', (request) => {
      request.reply({ body: searchResponse({ ...student, enabled }) })
    }).as('studentSearch')
    cy.intercept('PATCH', '**/students/4/deactivation', (request) => {
      enabled = false
      request.reply({ body: studentResponse({ ...student, enabled: false }) })
    }).as('deactivation')
    cy.visit('/students')
    cy.wait('@studentSearch')

    cy.get('[data-cy=student-deactivate]').then(($button) => {
      const click = () => $button[0].dispatchEvent(new MouseEvent('click', { bubbles: true }))
      click()
      click()
    })
    cy.wait('@studentDetails')
    cy.get('.el-message-box').contains('button', 'Deactivate').click()

    cy.wait('@deactivation').its('request.method').should('eq', 'PATCH')
    cy.wait('@studentSearch')
    cy.get('@studentDetails.all').should('have.length', 1)
    cy.get('@deactivation.all').should('have.length', 1)
    cy.get('[data-cy=student-disabled]').should('be.visible')
  })
})
